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
package com.ericsson.amcommonwfs.cleanup;

import static com.ericsson.amcommonwfs.util.Constants.VALUES_FILE_AGE_CUTOFF_IN_HOURS;
import static com.ericsson.amcommonwfs.util.Constants.VALUES_FILE_NAME_PATTERN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.io.filefilter.AgeFileFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanUpTemporaryValues {

    private final String pathToTempDirectory = System.getProperty("java.io.tmpdir");

    @Scheduled(cron = "0 0 * * * *")
    public void cleanUpTempValuesFiles() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusHours(VALUES_FILE_AGE_CUTOFF_IN_HOURS);
        long threshold = earlier.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AgeFileFilter fileFilter = new AgeFileFilter(threshold);
        File directory = new File(pathToTempDirectory);
        File[] files = directory.listFiles();
        if (files != null) {
            deleteValuesFile(files, fileFilter);
        }
    }

    private static void deleteValuesFile(File[] files, AgeFileFilter fileFilter) throws IOException {
        for (File f : files) {
            if (VALUES_FILE_NAME_PATTERN.matcher(f.getName()).matches() && !f.isDirectory() &&
                    fileFilter.accept(f)) {
                Path pathToFile = Paths.get(f.getAbsolutePath());
                Files.delete(pathToFile);
            }
        }
    }
}
