package com.vtc.openapi.ui.dto.open.task;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreateScanTaskByJsonRequestValidationTest {

    private final Validator validator;

    public CreateScanTaskByJsonRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void notBlankValidatorAvailableForTaskName() {
        CreateScanTaskByJsonRequest request = new CreateScanTaskByJsonRequest();
        request.setExtTaskId("ext-1");
        request.setType(1);
        ScanTaskTargetsDto targets = new ScanTaskTargetsDto();
        targets.setHosts("192.168.1.1");
        request.setTargets(targets);

        Set<ConstraintViolation<CreateScanTaskByJsonRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "taskName".equals(v.getPropertyPath().toString())));
    }
}
