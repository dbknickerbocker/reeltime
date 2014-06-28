package in.reeltime.transcoder.local

import in.reeltime.exceptions.TranscoderException
import in.reeltime.transcoder.TranscoderService
import in.reeltime.video.Video

class FfmpegTranscoderService implements TranscoderService {

    def localFileSystemService
    def ffmpeg

    def segmentDuration
    def segmentFormat

    @Override
    void transcode(Video video, String output){
        try {
            log.debug("Entering ${this.class.simpleName} transcode with video [${video.id}] and output [$output]")
            ensurePathToFfmpegIsDefined(ffmpeg)

            def outputPath = localFileSystemService.getAbsolutePathToOutputFile(output)
            def directory = localFileSystemService.createDirectory(outputPath)

            def playlist = video.hashCode() + '.m3u8'
            def segment = String.format(segmentFormat, video.hashCode())

            def videoPath = localFileSystemService.getAbsolutePathToInputFile(video.masterPath)
            def command = """${ffmpeg} -i ${videoPath} -vcodec libx264 -acodec libfaac -profile:v
                         |baseline -flags -global_header -map 0:0 -map 0:1 -f segment -segment_time ${segmentDuration}
                         |-segment_list ${playlist} -segment_format mpegts ${segment}""".stripMargin()

            log.debug("Excecuting command: $command")

            def process = command.execute(null, directory)
            process.waitFor()

            log.info("Completed ffmpeg transcoding for video [${video.id}]")
            log.debug(process.err.text)

            log.info("Making video [${video.id} available for streaming shortly")
            sleep(4 * 1000)
            video.available = true
        }
        catch(Exception e) {
            throw new TranscoderException(e)
        }
    }

    private static void ensurePathToFfmpegIsDefined(ffmpeg) {
        if(!ffmpeg)
            throw new IllegalStateException('ffmpeg could not be found')
    }
}
