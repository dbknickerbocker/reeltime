package in.reeltime.security

import in.reeltime.test.rest.RestRequest
import in.reeltime.test.spec.FunctionalSpec
import spock.lang.Unroll

class SpringSecurityCoreFunctionalSpec extends FunctionalSpec {

    @Unroll
    void "cannot access the form login regardless of params"() {
        given:
        def request = new RestRequest(url: urlFactory.springSecurityCheckUrl, customizer: params)

        when:
        def response = post(request)

        then:
        responseChecker.assertAuthError(response, 401, 'unauthorized', 'Full authentication is required to access this resource')

        where:
        _   |   params
        _   |   {}
        _   |   {username = 'bob'; password = 'pass'}
    }

    void "including a token makes no difference"() {
        given:
        def token = getAccessTokenWithScopeForTestUser('account-read')
        def request = new RestRequest(url: urlFactory.springSecurityCheckUrl, token: token)

        when:
        def response = post(request)

        then:
        response.status == 403
        response.json.error == 'access_denied'
        response.json.error_description == 'Access is denied'
    }
}
