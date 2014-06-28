package in.reeltime.notification

import helper.rest.RestRequest
import in.reeltime.FunctionalSpec
import spock.lang.Unroll

class NotificationControllerFunctionalSpec extends FunctionalSpec {

    @Unroll
    void "invalid http method [#method] for action [#action]"() {
        given:
        def notificationUrl = getUrlForResource("transcoder/notification/$action")
        def request = new RestRequest(url: notificationUrl)

        when:
        def response = "$method"(request)

        then:
        response.status == 405
        response.body == ''

        where:
        [action, method] << [['completed', 'progressing', 'warning', 'error'], ['get', 'put', 'delete']].combinations()
    }
}