package in.reeltime.transcoder.local

import in.reeltime.exceptions.TranscoderException
import in.reeltime.transcoder.TranscoderService
import in.reeltime.video.Video

class FfmpegTranscoderService implements TranscoderService {

    def localFileSystemService
    def playlistService

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
            def command = """${ffmpeg} -i ${videoPath} -s 480x270 -vcodec libx264 -acodec libfaac -profile:v
                         |baseline -flags -global_header -map 0:0 -map 0:1 -f segment -segment_time ${segmentDuration}
                         |-segment_list ${playlist} -segment_format mpegts ${segment}""".stripMargin()

            log.debug("Excecuting command: $command")

            def process = command.execute(null, directory)
            process.waitFor()

            log.info("Completed ffmpeg transcoding for video [${video.id}]")
            log.debug(process.err.text)

            def variantPlaylistKey = video.hashCode() + '-variant'
            def variant = variantPlaylistKey + '.m3u8'

            log.info("Generating variant playlist")
            generateVariantPlaylist(outputPath, variant, playlist)

            log.info("Making video [${video.id}] available for streaming")
            playlistService.addPlaylists(video, output + File.separator, variantPlaylistKey)
        }
        catch(Exception e) {
            throw new TranscoderException(e)
        }
    }

    private static void ensurePathToFfmpegIsDefined(ffmpeg) {
        if(!ffmpeg)
            throw new IllegalStateException('ffmpeg could not be found')
    }

    private static void generateVariantPlaylist(String outputPath, String variantPlaylist, String mediaPlaylist) {
        new File(outputPath, variantPlaylist).withWriter { BufferedWriter writer ->
            writer.writeLine('#EXTM3U')
            writer.writeLine('#EXT-X-STREAM-INF:PROGRAM-ID=1,RESOLUTION=480x270,CODECS="avc1.42001e,mp4a.40.2",BANDWIDTH=663000')
            writer.writeLine(mediaPlaylist)
        }
    }
}
