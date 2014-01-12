package in.reeltime.video

import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import in.reeltime.user.User
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import spock.lang.Specification

import in.reeltime.user.UserAuthenticationService

@TestFor(VideoController)
class VideoControllerSpec extends Specification {

    void "return 400 if video param is missing from request"() {
        when:
        controller.upload()

        then:
        response.contentType.contains('application/json')
        response.status == 400

        and:
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.message == 'Video is required'
    }

    void "return 400 if title param is missing from request"() {
        given:
        def videoParam = new GrailsMockMultipartFile('video', 'foo'.bytes)
        request.addFile(videoParam)

        when:
        controller.upload()

        then:
        response.contentType.contains('application/json')
        response.status == 400

        and:
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.message == 'Title is required'
    }

    void "return 201 after video has been uploaded with minimum params"() {
        given:
        def loggedInUser = new User(username: 'bob')
        controller.userAuthenticationService = Stub(UserAuthenticationService) {
            getLoggedInUser() >> loggedInUser
        }

        def videoParam = new GrailsMockMultipartFile('video', 'foo'.bytes)
        request.addFile(videoParam)

        def title = 'some title'
        params.title = title

        and:
        controller.videoService = Mock(VideoService)

        def validateArgs = { User u, String t, InputStream input ->
            assert u == loggedInUser
            assert t == title
            assert input.bytes == videoParam.inputStream.bytes
        }

        when:
        controller.upload()

        then:
        1 * controller.videoService.createVideo(_, _, _) >> { args -> validateArgs(args) }

        and:
        response.status == 201
    }
}