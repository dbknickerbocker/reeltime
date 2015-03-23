package in.reeltime.thumbnail

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import in.reeltime.video.Video

@ToString(includeNames = true)
@EqualsAndHashCode(includes = ['uri'])
class Thumbnail {

    ThumbnailResolution resolution
    String uri

    static belongsTo = [video: Video]

    static constraints = {
        resolution nullable: false
        uri nullable: false, blank: false, unique: true
        video nullable: false
    }
}