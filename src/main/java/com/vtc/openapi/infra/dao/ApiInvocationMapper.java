package com.vtc.openapi.infra.dao;

import com.botany.spore.mybatis.IBaseMapper;
import com.vtc.openapi.infra.dao.data.InvocationDailyStatRow;
import com.vtc.openapi.infra.dao.data.InvocationErrorCodeStatRow;
import com.vtc.openapi.infra.dao.po.ApiInvocationPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface ApiInvocationMapper extends IBaseMapper<ApiInvocationPO> {

    @Select("SELECT COUNT(1) FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND started_at >= #{from} AND started_at < #{to}")
    Long countByPartnerAndTimeRange(@Param("partnerId") String partnerId,
                                    @Param("from") Date from,
                                    @Param("to") Date to);

    @Select("SELECT COUNT(1) FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND started_at >= #{from} AND started_at < #{to} " +
            "AND response_code = 0")
    Long countSuccessByPartnerAndTimeRange(@Param("partnerId") String partnerId,
                                           @Param("from") Date from,
                                           @Param("to") Date to);

    @Select("SELECT response_code AS responseCode, COUNT(1) AS count " +
            "FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND started_at >= #{from} AND started_at < #{to} " +
            "AND response_code IS NOT NULL AND response_code <> 0 " +
            "GROUP BY response_code " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<InvocationErrorCodeStatRow> selectTopErrorCodes(@Param("partnerId") String partnerId,
                                                         @Param("from") Date from,
                                                         @Param("to") Date to,
                                                         @Param("limit") int limit);

    @Select("SELECT DATE(started_at) AS statDay, " +
            "COUNT(1) AS totalCount, " +
            "SUM(CASE WHEN response_code = 0 THEN 1 ELSE 0 END) AS successCount " +
            "FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND started_at >= #{from} AND started_at < #{to} " +
            "GROUP BY DATE(started_at) " +
            "ORDER BY statDay ASC")
    List<InvocationDailyStatRow> selectDailyStats(@Param("partnerId") String partnerId,
                                                  @Param("from") Date from,
                                                  @Param("to") Date to);

    @Select("SELECT COUNT(1) FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND (#{from} IS NULL OR started_at >= #{from}) " +
            "AND (#{to} IS NULL OR started_at < #{to})")
    Long countByPartnerWithRange(@Param("partnerId") String partnerId,
                                 @Param("from") Date from,
                                 @Param("to") Date to);

    @Select("SELECT COUNT(1) FROM api_invocation " +
            "WHERE partner_id = #{partnerId} " +
            "AND response_code = 0 " +
            "AND (#{from} IS NULL OR started_at >= #{from}) " +
            "AND (#{to} IS NULL OR started_at < #{to})")
    Long countSuccessByPartnerWithRange(@Param("partnerId") String partnerId,
                                        @Param("from") Date from,
                                        @Param("to") Date to);

    @Select("SELECT response_body_json FROM api_invocation WHERE invocation_id = #{invocationId}")
    String selectResponseBodyJson(@Param("invocationId") String invocationId);

    @Select("SELECT IFNULL(CHAR_LENGTH(response_body_json), 0) FROM api_invocation WHERE invocation_id = #{invocationId}")
    Long selectResponseBodyByteSize(@Param("invocationId") String invocationId);

    @Select("SELECT request_body_json FROM api_invocation WHERE invocation_id = #{invocationId}")
    String selectRequestBodyJson(@Param("invocationId") String invocationId);

    @Select("SELECT IFNULL(CHAR_LENGTH(request_body_json), 0) FROM api_invocation WHERE invocation_id = #{invocationId}")
    Long selectRequestBodyByteSize(@Param("invocationId") String invocationId);
}
