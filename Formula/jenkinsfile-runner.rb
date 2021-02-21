class JenkinsfileRunner < Formula
    desc "A command line tool to run Jenkinsfiles"
    homepage "https://github.com/jenkinsci/jenkinsfile-runner"
    head "https://github.com/jenkinsci/jenkinsfile-runner.git"

    bottle :unneeded
    depends_on :java => "1.8"
    depends_on "maven"

    def install
        system "mvn package"
        libexec.install Dir["*"]
        bin.install_symlink libexec/"app/target/appassembler/bin/jenkinsfile-runner"
    end
end
