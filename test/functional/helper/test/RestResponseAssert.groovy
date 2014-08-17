package helper.test

import grails.plugins.rest.client.RestResponse
import helper.rest.AuthorizationAwareRestClient
import helper.rest.RestRequest

import static helper.rest.HttpContentTypes.APPLICATION_JSON
import static helper.rest.HttpHeaders.CONTENT_TYPE
import static helper.rest.HttpHeaders.WWW_AUTHENTICATE

class RestResponseAssert {

    private AuthorizationAwareRestClient restClient

    RestResponseAssert(AuthorizationAwareRestClient restClient) {
        this.restClient = restClient
    }

    void assertInvalidHttpMethods(String url, Collection<String> methods, String token = null) {
        methods.each { String method ->
            println "HTTP Method: $method"
            def request = new RestRequest(url: url, token: token)
            def response = restClient."$method"(request) as RestResponse

            if(token) {
                assert response.status == 403
                assert response.json.error == 'access_denied'
                assert response.json.error_description == 'Access is denied'
            }
            else {
                assert response.status == 401

                // There's some weirdness with the json accessor on the RestResponse returning
                // null despite the HTTP response containing valid JSON
                if(method.toLowerCase() != 'put') {
                    assert response.json.error == 'unauthorized'
                    assert response.json.error_description == 'Full authentication is required to access this resource'
                }
            }
        }
    }

    void assertSingleErrorMessageResponse(RestResponse response, int expectedStatus, String expectedMessage) {
        assert response.status == expectedStatus
        assert response.json.errors.size() == 1
        assert response.json.errors[0] == expectedMessage
    }

    void assertStatusCode(RestResponse response, int expected) {
        assert response.status == expected
    }

    void assertContentType(RestResponse response, String expected) {
        def contentType = response.headers.get(CONTENT_TYPE)[0]
        assert contentType.startsWith(expected)
    }

    void assertAuthError(RestResponse response, int status, String error, String description) {
        assertStatusCode(response, status)
        assertContentType(response, APPLICATION_JSON)

        def wwwAuthenticate = response.headers.get(WWW_AUTHENTICATE)[0]
        assert wwwAuthenticate.contains("error=\"$error\"")
        assert wwwAuthenticate.contains("error_description=\"$description\"")
    }
}
