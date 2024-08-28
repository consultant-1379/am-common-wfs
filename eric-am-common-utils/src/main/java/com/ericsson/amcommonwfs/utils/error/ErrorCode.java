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
package com.ericsson.amcommonwfs.utils.error;

import static com.ericsson.amcommonwfs.utils.CommonUtils.convertToJSONString;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RESOURCE_ALREADY_EXISTS;
import static com.ericsson.amcommonwfs.utils.constants.Constants.RESOURCE_TYPE_ALREADY_EXISTS;

import java.util.regex.Matcher;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements ErrorOutputTranslator {

    BPMN_IO_EXCEPTION("error.com.io.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_INSTALL_FAILED("error.com.install.failed") {
        @Override
        public String translate(final String commandOutput) {
            String errorOutput = commandOutput;
            Matcher resourceAlreadyExists = RESOURCE_ALREADY_EXISTS.matcher(commandOutput);
            Matcher resourceTypeAlreadyExists = RESOURCE_TYPE_ALREADY_EXISTS.matcher(commandOutput);
            if (resourceAlreadyExists.find()) {
                errorOutput = "A resource named " + resourceAlreadyExists.group(1)
                        + " already exists. Please use a different name or delete the resource with this name.";
            } else if (resourceTypeAlreadyExists.find()) {
                errorOutput = "Lifecycle operation of resource: " + resourceTypeAlreadyExists.group(1)
                        + " failed due to " + resourceTypeAlreadyExists.group(2)
                        + " already being present in the namespace. Please use a different namespace or remove the duplicate resource";
            }

            return convertToJSONString(errorOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_UPGRADE_FAILED("error.com.upgrade.failed") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_SCALE_FAILED("error.com.scale.failed") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_ROLLBACK_FAILED("error.com.rollback.failed") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_CRD_FAILED("error.com.crd.failed") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_INTERRUPTED_EXCEPTION("error.com.interrupted.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_UNKNOWN_EXCEPTION("error.com.unknown.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_COMMAND_TIMEOUT_EXCEPTION("error.com.command.timedOut.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_INVALID_ARGUMENT_EXCEPTION("error.com.invalid.argument.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_INVALID_RESPONSE_EXCEPTION("error.com.invalid.response") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_DELETION_FAILURE("error.com.deletion.failure") {
        @Override
        public String translate(final String commandOutput) {
            String errorOutput = commandOutput;
            if (commandOutput == null || commandOutput.contains("no matching resources found")) {
                errorOutput = "Deletion of the resource timed out. It may complete in the background on the cluster. "
                        + "You can clean up the resource on the UI";
            }
            return convertToJSONString(errorOutput, HttpStatus.INTERNAL_SERVER_ERROR.toString());
        }
    },
    BPMN_POD_STATUS_FAILURE("error.com.pod.status.failure") {
        @Override
        public String translate(final String commandOutput) {
            String errorOutput = commandOutput;
            if (commandOutput == null || commandOutput.contains("timed out waiting for the condition")) {
                errorOutput = "The lifecycle operation on the resource timed out. It may complete in the background "
                        + "on the cluster. You can clean up the resource on the UI";
            }
            return convertToJSONString(errorOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },

    BPMN_HISTORY_FAILURE("error.com.history.failure") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_STATUS_FAILURE("error.com.status.failure") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_GET_FAILURE("error.com.get.failure") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_APPLICATION_DEPLOYED_TIMEOUT_EXCEPTION("error.com.application.deployed.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_APPLICATION_CONTAINERS_TIMEOUT_EXCEPTION("error.com.application.containers.state.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_APPLICATION_TERMINATION_TIMEOUT_EXCEPTION("error.com.application.termination.exception") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_CLUSTER_CONFIG_NOT_PRESENT("error.com.cluster.config.not.present") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_REQUIRED_LABEL_NOT_PRESENT("error.com.missing.required.label") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_KUBECTL_FAILURE("error.com.kubectl.failure") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    LONG_RUNNING_COMMAND_REQUEST_FAILURE("error.com.long.running.command.request.failure") {
        @Override
        public String translate(final String commandOutput) {
            return convertToJSONString(commandOutput, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_CREATE_SECRET_FAILED("error.com.create.secret.failure") {
        @Override
        public String translate(final String response) {
            return convertToJSONString(response, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_DELETE_SECRET_FAILED("error.com.delete.secret.failure") {
        @Override
        public String translate(final String response) {
            return convertToJSONString(response, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_DELETE_PVC_FAILED("error.com.delete.pvc.failed") {
        @Override
        public String translate(final String response) {
            return convertToJSONString(response, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    },
    BPMN_DEPENDENCY_SERVICE_UNAVAILABLE("error.com.dependency.service.unavailable") {
        @Override
        public String translate(final String response) {
            return convertToJSONString(response, HttpStatus.SERVICE_UNAVAILABLE.toString());
        }
    };

    private String error;

    ErrorCode(final String error) {
        this.error = error;
    }

    public String getErrorCodeAsString() {
        return error;
    }

    public static ErrorCode getErrorCode(final String error) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.error.equalsIgnoreCase(error)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException(error + " is not a valid errorCode");
    }
}
