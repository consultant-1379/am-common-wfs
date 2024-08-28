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
package com.ericsson.amcommonwfs.presentation.converter;

/**
 * Converter api for presentation layer conversion.
 * @param <R> - conversion result object type.
 * @param <P> - object type to be converted.
 */
public interface Converter<R, P> {

    /**
     * Method that converts an object of a certain type into an object of another type.
     * The method does not imply transfer of a previously created object, this means that
     * the output object must be created inside the method. It is not required to support a null parameter,
     * it is also assumed that the method can throw any runtime errors related to data conversion.
     * @param parameter - object to be converted.
     * @return - object converted based on input object data.
     */
    R convert(P parameter);

}
