//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.client;

import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

/**
 * Util class for mocking.
 */
public final class MockUtil {
    // private constructor for Util class.
    private MockUtil() { }

    static HttpURLConnection getMockedConnectionWithSuccessResponse(final String message) throws IOException {
        final HttpURLConnection mockedHttpUrlConnection  = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenReturn(Util.createInputStream(message));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        return mockedHttpUrlConnection;
    }

    static HttpURLConnection getMockedConnectionWithFailureResponse(final int statusCode, final String errorMessage)
            throws IOException {
        final HttpURLConnection mockedHttpUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenThrow(IOException.class);
        Mockito.when(mockedHttpUrlConnection.getErrorStream()).thenReturn(Util.createInputStream(errorMessage));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(statusCode);

        return mockedHttpUrlConnection;
    }

    static HttpURLConnection getMockedConnectionWithSocketTimeout() throws IOException {
        final HttpURLConnection mockedUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedUrlConnection.getInputStream()).thenThrow(SocketTimeoutException.class);
        return mockedUrlConnection;
    }

    static HttpURLConnection getCommonHttpUrlConnection() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doNothing().when(mockedConnection).setConnectTimeout(Mockito.anyInt());
        Mockito.doNothing().when(mockedConnection).setDoInput(Mockito.anyBoolean());
        return mockedConnection;
    }
}
