package in.reeltime.video

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import in.reeltime.metadata.StreamMetadata
import spock.lang.Specification
import in.reeltime.storage.PathGenerationService
import in.reeltime.user.User
import in.reeltime.storage.InputStorageService
import in.reeltime.transcoder.TranscoderService
import in.reeltime.metadata.StreamMetadataService
import spock.lang.Unroll

@TestFor(VideoCreationService)
@Mock([Video])
class VideoCreationServiceSpec extends Specification {

    StreamMetadataService streamMetadataService

    void setup() {
        streamMetadataService = Mock(StreamMetadataService)

        service.inputStorageService = Mock(InputStorageService)
        service.pathGenerationService = Mock(PathGenerationService)
        service.transcoderService = Mock(TranscoderService)
        service.streamMetadataService = streamMetadataService
    }

    void "store video stream, save the video object and then transcode it"() {
        given:
        def creator = new User(username: 'bob')
        def title = 'fun times'
        def videoStream = new ByteArrayInputStream('yay'.bytes)

        and:
        def masterPath = 'foo'
        def outputPath = 'bar'

        and:
        def validateTranscodeVideoArgs = { Video v ->
            assert v.creator == creator
            assert v.title == title
            assert v.masterPath == masterPath
        }

        when:
        def video = service.createVideo(creator, title, videoStream)

        then:
        1 * service.pathGenerationService.getUniqueInputPath() >> masterPath
        1 * service.inputStorageService.store(videoStream, masterPath)

        and:
        1 * service.pathGenerationService.getUniqueOutputPath() >> outputPath
        1 * service.transcoderService.transcode(_ as Video, outputPath) >> { args -> validateTranscodeVideoArgs(args[0])}

        and:
        video.creator == creator
        video.title == title
        video.masterPath == masterPath
    }

    @Unroll
    void "do not allow videos that contain a stream with an invalid duration [#duration]"() {
        given:
        def command = new VideoCreationCommand()

        when:
        def allowed = service.allowCreation(command)

        then:
        !allowed

        and:
        1 * streamMetadataService.extractStreams(_) >> [new StreamMetadata(duration: duration)]

        where:
        _   |   duration
        _   |   ''
        _   |   '.0124'
        _   |   '124.'
        _   |   '9000.asf'
        _   |   '1234#1441'
    }

    @Unroll
    void "do not allow videos that contain a stream that exceeds max duration [#duration]"() {
        given:
        grailsApplication.config.reeltime.metadata.maxDurationInSeconds = max

        and:
        def command = new VideoCreationCommand()

        when:
        def allowed = service.allowCreation(command)

        then:
        !allowed

        and:
        1 * streamMetadataService.extractStreams(_) >> [new StreamMetadata(duration: duration)]

        where:
        max     |   duration
        9000    |   '9000.000000'
        9000    |   '9000.000001'
        9000    |   '9000.123456'
    }
}
