package io.jenkins.jenkinsfile.runner.vanilla;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class IntegrationTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog();

    @Rule
    public final SystemErrRule systemErr = new SystemErrRule().enableLog();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(120);


    private static final String ELASTICSEARCH_VERSION = "7.9.2";
    private static final DockerImageName ELASTICSEARCH_IMAGE =
            DockerImageName
                    .parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag(ELASTICSEARCH_VERSION);
    final String ELASTICSEARCH_USERNAME = "admin";

    final String ELASTICSEARCH_PASSWORD = "admin";

    @Test
    public void testHttpRequestPlugin() throws Throwable {
        
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD));

            RestClient client = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                    .build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));

            // Test Jenkins Pipeline
            final String address = container.getHttpHostAddress() + "/_cluster/health";
            File jenkinsfile = tmp.newFile("Jenkinsfile");
            FileUtils.writeStringToFile(jenkinsfile, "node() {\n" +
            "  def response = httpRequest 'http://" + address + "' \n" +
            "  echo response.content \n" +
            "  def foo = readJSON(text: response.content) \n" +
            "  echo 'Status is ' + foo.status \n" +
            "}", Charset.defaultCharset());

            int result = new JFRTestUtil().runAsCLI(jenkinsfile);
            assertThat("JFR should be executed successfully", result, equalTo(0));
            assertThat(systemOut.getLog(), containsString("Status is green"));
        }
    }

}
