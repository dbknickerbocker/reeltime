package in.reeltime.mail.aws

import grails.transaction.Transactional
import in.reeltime.mail.MailServerExistenceService
import org.xbill.DNS.Lookup
import org.xbill.DNS.MXRecord
import org.xbill.DNS.Record
import org.xbill.DNS.Type

@Transactional
class MXRecordMailServerExistenceService implements MailServerExistenceService {

    boolean exists(String host) {
        boolean exists = false

        try {
            Record[] records = new Lookup(host, Type.MX).run()
            exists = records?.length > 0

            records.each { MXRecord record ->
                log.debug "Found MXRecord: [$record]"
            }
        }
        catch (Exception e) {
            log.debug "Failed to lookup MX records for host [$host]", e
        }

        return exists
    }
}
