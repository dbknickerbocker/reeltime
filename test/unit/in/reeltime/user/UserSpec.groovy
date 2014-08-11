package in.reeltime.user

import grails.plugin.springsecurity.SpringSecurityService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import in.reeltime.oauth2.Client
import in.reeltime.reel.Reel
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(User)
@Mock([Client])
class UserSpec extends Specification {

    @Unroll
    void "username [#username] is valid [#valid]"() {
        given:
        def user = new User(username: username)

        expect:
        user.validate(['username']) == valid

        where:
        username        |   valid
        null            |   false
        ''              |   false
        'a'             |   false
        '!a'            |   false
        '!ab'           |   false
        'w' * 14 + '!'  |   false
        'r' * 16        |   false

        'xy'            |   true
        'abcde'         |   true
        'abcdef'        |   true
        'Ab2C01faqWZ'   |   true
        'r' * 15        |   true
    }

    @Unroll
    void "email [#email] is valid [#valid]"() {
        given:
        def user = new User(email: email)

        expect:
        user.validate(['email']) == valid

        where:
        email               |   valid
        null                |   false
        ''                  |   false
        'oops'              |   false
        'foo@'              |   false
        'foo@b'             |   false
        '@coffee'           |   false
        'foo@bar.com'       |   true
        'foo@bar.baz.buzz'  |   true
    }

    @Unroll
    void "clients list cannot be null"() {
        given:
        def user = new User(clients: null)

        expect:
        !user.validate(['clients'])
    }

    @Unroll
    void "[#count] clients is valid [#valid]"() {
        given:
        def clients = createClients(count)
        def user = new User(clients: clients)

        expect:
        user.validate(['clients']) == valid

        where:
        count   |   valid
        0       |   false
        1       |   true
        2       |   false
        3       |   false
    }

    void "reels list cannot be null"() {
        given:
        def user = new User(reels: null)

        expect:
        !user.validate(['reels'])
    }

    @Unroll
    void "user has reel [#reelToCheck] [#truth] when reel [#reelToAdd] is the only reel"() {
        given:
        def reel = new Reel(name: reelToAdd)
        def user = new User(reels: [reel])

        expect:
        user.hasReel(reelToCheck) == truth

        where:
        reelToAdd   |   reelToCheck     |   truth
        'something' |   'something'     |   true
        'something' |   'nothing'       |   false
    }

    @Unroll
    void "[#count] reels is valid [#valid]"() {
        given:
        def reels = createReels(count)
        def user = new User(reels: reels)

        expect:
        user.validate(['reels']) == valid

        where:
        count   |   valid
        0       |   false
        1       |   true
        2       |   true
        10      |   true
        100     |   true
    }

    private Collection<Client> createClients(int count) {
        def clients = []
        count.times { clients << createClient() }
        return clients
    }

    private Client createClient() {
        def client = new Client()
        client.springSecurityService = Stub(SpringSecurityService)
        client.save(validate: false)
        return client
    }

    private static Collection<Reel> createReels(int count) {
        def reels = []
        count.times { reels << new Reel() }
        return reels
    }
}
