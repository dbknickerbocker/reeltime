package in.reeltime.video

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import spock.lang.Specification

import in.reeltime.user.UserAuthenticationService

@TestFor(VideoController)
class VideoControllerSpec extends Specification {

    void "return 401 if attempting to upload video without being logged in"() {
        given:
        mockUserAuthenticationService(false)

        when:
        controller.upload()

        then:
        response.status == 401
    }

    void "return 400 if video param is missing from request"() {
        given:
        mockUserAuthenticationService(true)

        when:
        controller.upload()

        then:
        response.status == 400
    }

    void "return 201 after video has been uploaded"() {
        given:
        mockUserAuthenticationService(true)

        def video = new GrailsMockMultipartFile('video', 'foo'.bytes)
        request.addFile(video)

        and:
        controller.videoSubmissionService = Mock(VideoSubmissionService)

        def validateArgs = { InputStream input ->
            assert input.bytes == video.inputStream.bytes
        }

        when:
        controller.upload()

        then:
        1 * controller.videoSubmissionService.submit(_) >> { args -> validateArgs(args) }

        and:
        response.status == 201
    }

    private void mockUserAuthenticationService(boolean isLoggedIn) {
        controller.userAuthenticationService = Mock(UserAuthenticationService) {
            1 * isUserLoggedIn() >> isLoggedIn
        }
    }
}
