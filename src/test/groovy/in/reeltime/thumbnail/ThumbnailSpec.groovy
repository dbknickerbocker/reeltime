package in.reeltime.thumbnail

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(Thumbnail)
class ThumbnailSpec extends Specification {

    @Unroll
    void "[#key] cannot be [#value]"() {
        given:
        def thumbnail = new Thumbnail((key): value)

        expect:
        !thumbnail.validate([key])

        where:
        key             |   value
        'uri'           |   null
        'uri'           |   ''
        'resolution'    |   null
    }

    @Unroll
    void "valid resolution [#resolution.name()] specified"() {
        given:
        def thumbnail = new Thumbnail(resolution: resolution)

        expect:
        thumbnail.validate(['resolution'])

        where:
        _   |   resolution
        _   |   ThumbnailResolution.RESOLUTION_1X
        _   |   ThumbnailResolution.RESOLUTION_2X
        _   |   ThumbnailResolution.RESOLUTION_3X
    }
}
