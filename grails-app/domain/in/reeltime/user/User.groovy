package in.reeltime.user

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import in.reeltime.exceptions.ReelNotFoundException
import in.reeltime.video.Video
import in.reeltime.oauth2.Client
import in.reeltime.reel.Reel
import in.reeltime.reel.Audience

@ToString(includeNames = true, includes = ['displayName', 'username'])
@EqualsAndHashCode(includes = ['username'])
class User {

    static final USERNAME_REGEX = /^\w{2,15}$/
    static final DISPLAY_NAME_REGEX = /^\w{1}[\w ]{0,18}?\w{1}$/
    static final PASSWORD_MIN_SIZE = 6

	transient springSecurityService

    String displayName
	String email
	String username
	String password
	boolean verified
	boolean enabled = true
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired

    static hasMany = [videos: Video, clients: Client, reels: Reel]

	static transients = [
            'springSecurityService',
            'numberOfFollowees',
            'numberOfFollowers',
            'numberOfReels',
            'numberOfAudienceMemberships',
            'currentUserIsFollowing'
    ]

    static List<User> findAllByIdInListInAlphabeticalOrderByPage(List<Long> userIds, int page, int maxUsersPerPage) {
        int offset = (page - 1) * maxUsersPerPage
        User.findAllByIdInList(userIds, [max: maxUsersPerPage, offset: offset, sort: 'username'])
    }

    static List<Long> findAllClientIdsByUser(User user) {
        User.withCriteria {
            idEq(user.id)
            clients {
                projections {
                    property('id')
                }
            }
        } as List<Long>
    }

	static constraints = {
        displayName blank: false, nullable: false, matches: DISPLAY_NAME_REGEX
        email blank: false, nullable: false, email: true
		username blank: false, nullable: false, matches: USERNAME_REGEX, unique: true
		password blank: false, nullable: false
        clients nullable: false
        reels nullable: false, minSize: 1, validator: reelsValidator
	}

	static mapping = {
		password column: '`password`'
	}

    static Closure reelsValidator = { val, obj ->
        withNewSession {
            return obj.hasReel(Reel.UNCATEGORIZED_REEL_NAME)
        }
    }

    boolean hasReel(String reelName) {
        return findReelByName(reelName) != null
    }

    Reel getReel(String reelName) {
        def reel = findReelByName(reelName)
        if(!reel) {
            throw new ReelNotFoundException("User [$username] does not have reel named [$reelName]")
        }
        return reel
    }

    private Reel findReelByName(String reelName) {
        if(User.exists(id)) {
            return Reel.findByOwnerAndName(this, reelName)
        }
        else {
            return reels.find { reel -> reel.name == reelName }
        }
    }

    int getNumberOfFollowees() {
        UserFollowing.countByFollower(this)
    }

    int getNumberOfFollowers() {
        UserFollowing.countByFollowee(this)
    }

    int getNumberOfReels() {
        Reel.countByOwner(this)
    }

    int getNumberOfAudienceMemberships() {
        Audience.countByAudienceMember(this)
    }

    boolean getCurrentUserIsFollowing() {
        def currentUser = springSecurityService.currentUser as User
        UserFollowing.findByFollowerAndFollowee(currentUser, this) != null
    }

    Set<Role> getAuthorities() {
		UserRole.findAllByUser(this).collect { it.role } as Set
	}

	def beforeInsert() {
		encodePassword()
	}

	def beforeUpdate() {
		if (isDirty('password')) {
			encodePassword()
		}
	}

	protected void encodePassword() {
		password = springSecurityService.encodePassword(password)
	}
}
