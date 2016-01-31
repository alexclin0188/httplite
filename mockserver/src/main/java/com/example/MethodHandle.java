package com.example;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

/**
 * com.example
 *
 * @author alexclin
 * @date 16/1/4 22:02
 */
public interface MethodHandle {
    MockResponse handle(RecordedRequest request, String root);
}
