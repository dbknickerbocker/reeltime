package in.reeltime.aws

import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoder
import com.amazonaws.services.elastictranscoder.AmazonElasticTranscoderClient
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.TransferManager
import grails.plugin.spock.IntegrationSpec
import org.springframework.beans.factory.InitializingBean
import spock.lang.Unroll

class AwsServiceIntegrationSpec extends IntegrationSpec {

    def awsService

    void "must be an InitializingBean"() {
        expect:
        awsService instanceof InitializingBean
    }

    @Unroll
    void "createClient for [#interfaceClass] returns an instance of [#clientClass]"() {
        expect:
        awsService.createClient(interfaceClass).class == clientClass

        where:
        interfaceClass              |   clientClass
        AmazonElasticTranscoder     |   AmazonElasticTranscoderClient
        AmazonS3                    |   AmazonS3Client
    }

    void "createTransferManager is backed by an AmazonS3Client"() {
        when:
        def transferManger = awsService.createTransferManager()

        then:
        transferManger instanceof TransferManager
        transferManger.amazonS3Client instanceof AmazonS3Client
    }
}