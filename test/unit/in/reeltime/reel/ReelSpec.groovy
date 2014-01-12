package in.reeltime.reel

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(Reel)
class ReelSpec extends Specification {

    void "a reel must have one audience"() {
        given:
        def audience = new Audience()

        when:
        def reel = new Reel(audience: audience)

        then:
        reel.validate()
    }
}