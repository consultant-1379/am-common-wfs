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
package com.ericsson.amcommonwfs.presentation.repositories;

import com.ericsson.amcommonwfs.model.entity.RequestProcessingDetails;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RequestProcessingDetailsMapper {

    @Insert("INSERT INTO wfs.request_processing_details (request_id, request_hash, response_code, response_headers, " +
        "response_body, processing_state, retry_after, creation_time) " +
        "VALUES (#{id}, #{requestHash}, #{responseCode}, #{responseHeaders}, " +
        "#{responseBody}, #{processingState, jdbcType=OTHER}, " +
        "#{retryAfter}, #{creationTime})")
    int insertRequestProcessingDetails(RequestProcessingDetails requestProcessingDetails);

    @Update("UPDATE wfs.request_processing_details set response_headers = #{responseHeaders}, "
        + "response_body = #{responseBody}, "
        + "processing_state = #{processingState, jdbcType=OTHER}, "
        + "creation_time = #{creationTime}, response_code = #{responseCode} "
        + "WHERE request_id = #{id}")
    int updateRequestProcessingDetails(RequestProcessingDetails requestProcessingDetails);

    @Results(id = "requestProcessingDetailsMap", value = {
        @Result(property = "id", column = "request_id"),
        @Result(property = "requestHash", column = "request_hash"),
        @Result(property = "responseCode", column = "response_code"),
        @Result(property = "responseHeaders", column = "response_headers"),
        @Result(property = "responseBody", column = "response_body"),
        @Result(property = "processingState", column = "processing_state"),
        @Result(property = "retryAfter", column = "retry_after"),
        @Result(property = "creationTime", column = "creation_time")
    })
    @Select("SELECT * FROM wfs.request_processing_details WHERE request_id = #{id}")
    RequestProcessingDetails findById(String id);

    @Delete("DELETE FROM wfs.request_processing_details WHERE creation_time < #{permittedLastUpdateTime}")
    int deleteExpiredRequestProcessingDetails(LocalDateTime permittedLastUpdateTime);
}
