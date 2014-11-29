package in.reeltime.transcoder

import grails.test.spock.IntegrationSpec
import in.reeltime.video.Video
import spock.lang.IgnoreIf
import test.helper.UserFactory

class FfmpegTranscoderServiceIntegrationSpec extends IntegrationSpec {

    def ffmpegTranscoderService

    def grailsApplication

    def pathGenerationService
    def inputStorageService

    @IgnoreIf({!System.getProperty('ffmpeg') && !System.getenv('FFMPEG')})
    void "transcode video file using ffmpeg"() {
        given:
        def creator = UserFactory.createTestUser()

        def masterPath = pathGenerationService.uniqueInputPath
        def video = new Video(creator: creator, title: 'change peter parker', masterPath:  masterPath).save()

        and:
        def videoFilePath = 'test/files/spidey.mp4'
        storeTestVideo(masterPath, videoFilePath)

        and:
        def outputPath = pathGenerationService.uniqueOutputPath

        when:
        ffmpegTranscoderService.transcode(video, outputPath)

        then:
        video.available
        video.playlists.size() == 1

        and:
        assertDirectoryContainsPlaylistAndSegments(outputPath)

        cleanup:
        creator.delete()

        and:
        def outputDirectoryPath = grailsApplication.config.reeltime.storage.output as String
        new File(outputDirectoryPath).deleteOnExit()
    }

    private void storeTestVideo(String storagePath, String filePath) {
        new File(filePath).withInputStream { videoStream ->
            inputStorageService.store(videoStream, storagePath)
        }
    }

    private void assertDirectoryContainsPlaylistAndSegments(String path) {
        def output = grailsApplication.config.reeltime.storage.output
        def fullPath = "$output${File.separator}$path"

        def directory = new File(fullPath)
        assert directory.isDirectory()
        assert directory.listFiles().find { it.name.endsWith('.m3u8') }
        assert directory.listFiles().count { it.name.endsWith('.ts') } > 0
    }
}
