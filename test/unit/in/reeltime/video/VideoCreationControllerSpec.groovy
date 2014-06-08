package in.reeltime.video

import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import in.reeltime.user.User
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import spock.lang.Specification

import in.reeltime.user.UserAuthenticationService

@TestFor(VideoCreationController)
class VideoCreationControllerSpec extends Specification {

    User loggedInUser
    VideoCreationService videoCreationService

    void setup() {
        loggedInUser = new User(username: 'bob')
        controller.userAuthenticationService = Stub(UserAuthenticationService) {
            getLoggedInUser() >> loggedInUser
        }

        videoCreationService = Mock(VideoCreationService)
        controller.videoCreationService = videoCreationService
    }

    void "return 400 if video param is missing from request"() {
        when:
        controller.upload()

        then:
        response.contentType.contains('application/json')
        response.status == 400

        and:
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.message == '[video] is required'
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
        json.message == '[title] is required'
    }

    void "return 201 after video has been uploaded with minimum params"() {
        given:
        def videoData = 'foo'.bytes
        def videoParam = new GrailsMockMultipartFile('video', videoData)
        request.addFile(videoParam)

        def title = 'some title'
        params.title = title

        and:
        def validateCommand = { VideoCreationCommand command ->
            assert command.creator == loggedInUser
            assert command.title == title
            assert command.videoStream.bytes == videoData
        }

        def allowCommand = { VideoCreationCommand command ->
            validateCommand(command)
            command.videoStream = new ByteArrayInputStream(videoData)
            return true
        }

        when:
        controller.upload()

        then:
        1 * videoCreationService.allowCreation(_) >> { command -> allowCommand(command) }
        1 * videoCreationService.createVideo(_) >> { command -> validateCommand(command) }

        and:
        response.status == 201
    }
}
