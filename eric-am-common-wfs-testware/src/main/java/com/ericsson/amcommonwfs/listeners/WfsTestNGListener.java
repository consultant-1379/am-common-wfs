/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WfsTestNGListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        LOGGER.info("Entering test :: {}", method.toString());
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        LOGGER.info("Exiting test :: {}, Passed :: {}", method.getTestMethod().getMethodName(),
                method.getTestResult().isSuccess());
        if (testResult.getThrowable() != null) {
            String message = String.format("Exception occurred in method :: %s", testResult.getMethod().getMethodName());
            LOGGER.error(message, testResult.getThrowable());
        }
    }
}
