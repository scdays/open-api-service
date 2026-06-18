package com.vtc.openapi.infra.adapter.taskcenter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * 从 common.ftp SFTP 下载 VTC 原始扫描报告。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSftpReportDownloader {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSftpReportDownloader.class);

    private final String host;
    private final int port;
    private final String user;
    private final String password;

    public TaskCenterSftpReportDownloader(@Value("${common.ftp.host:}") String host,
                                          @Value("${common.ftp.port:22}") int port,
                                          @Value("${common.ftp.user:}") String user,
                                          @Value("${common.ftp.password:}") String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public byte[] download(String downloadPath) {
        TaskCenterReportPathSupport.ParsedReportPath parsed =
                TaskCenterReportPathSupport.parse(downloadPath);
        if (parsed == null) {
            throw new IllegalArgumentException("无效的 SFTP 报告路径: " + downloadPath);
        }
        if (!StringUtils.hasText(host) || !StringUtils.hasText(user)) {
            throw new IllegalStateException("common.ftp 未配置，无法下载扫描报告");
        }
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = openSession();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            sftp.setFilenameEncoding("UTF-8");
            if (StringUtils.hasText(parsed.getRemoteDir())) {
                sftp.cd(parsed.getRemoteDir());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sftp.get(parsed.getFileName(), out);
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new IllegalStateException("SFTP 报告为空: " + downloadPath);
            }
            log.info("task-center sftp report downloaded path={} bytes={}", downloadPath, bytes.length);
            return bytes;
        } catch (Exception ex) {
            throw new IllegalStateException("SFTP 报告下载失败: " + downloadPath + " — " + ex.getMessage(), ex);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private Session openSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(15000);
        return session;
    }
}
