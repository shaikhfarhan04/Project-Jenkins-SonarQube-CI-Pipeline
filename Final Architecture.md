Project: Jenkins + SonarQube CI Pipeline

## 1. Final Architecture

```text
                    ┌──────────────────────┐
                    │       GitHub         │
                    │ Java/Maven Project   │
                    └──────────┬───────────┘
                               │
                               │ Git Push
                               ▼
                    ┌──────────────────────┐
                    │      Jenkins EC2     │
                    │                      │
                    │ Jenkins              │
                    │ Java 21              │
                    │ Maven                │
                    │ Git                  │
                    └──────────┬───────────┘
                               │
                               │ Checkout Code
                               │ Build/Test
                               │ Sonar Scan
                               ▼
                    ┌──────────────────────┐
                    │    SonarQube EC2     │
                    │                      │
                    │ SonarQube            │
                    │ PostgreSQL            │
                    │ SonarScanner CLI      │
                    └──────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │ SonarQube Dashboard  │
                    │                      │
                    │ Bugs                 │
                    │ Vulnerabilities      │
                    │ Code Smells          │
                    │ Coverage             │
                    │ Quality Gate         │
                    └──────────────────────┘
```

---

# 2. Project Components

We will create:

| Component    | Technology        |
| ------------ | ----------------- |
| Cloud        | AWS               |
| Servers      | EC2               |
| Source Code  | GitHub            |
| CI/CD        | Jenkins           |
| Code Quality | SonarQube         |
| Scanner      | SonarScanner CLI  |
| Application  | Java              |
| Build        | Maven             |
| Database     | PostgreSQL        |
| OS           | Amazon Linux 2023 |

You can also do this with Ubuntu, but since your recent labs use **Amazon Linux 2023**, I'll use that.

---

# 3. AWS Infrastructure

Create **two EC2 instances**.

### EC2-1: Jenkins

Recommended:

```text
Name: Jenkins-Server
Instance: t3.medium
OS: Amazon Linux 2023
Storage: 20-30 GB
```

### EC2-2: SonarQube

```text
Name: SonarQube-Server
Instance: t3.medium
OS: Amazon Linux 2023
Storage: 30-40 GB
```

For a heavier Java/SonarQube project, `t3.large` is preferable.

---

# 4. Security Groups

## Jenkins Security Group

Inbound:

| Type    | Port | Source                    |
| ------- | ---: | ------------------------- |
| SSH     |   22 | Your IP                   |
| Jenkins | 8080 | Your IP / trusted network |
| HTTP    |   80 | Optional                  |

Jenkins URL:

```text
http://JENKINS_PUBLIC_IP:8080
```

---

# 5. SonarQube Security Group

Inbound:

| Type      | Port | Source                           |
| --------- | ---: | -------------------------------- |
| SSH       |   22 | Your IP                          |
| SonarQube | 9000 | Jenkins Security Group / your IP |

SonarQube URL:

```text
http://SONARQUBE_PUBLIC_IP:9000
```

### Important

For a real deployment, don't expose port `9000` to `0.0.0.0/0`.

For your lab, you can temporarily allow:

```text
TCP 9000
0.0.0.0/0
```

but restrict it afterward.

---

# 6. Project Repository

Create your own GitHub repository, for example:

```text
sonarqube-jenkins-project
```

Recommended structure:

```text
sonarqube-jenkins-project/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── example/
│   │               └── App.java
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── AppTest.java
│
├── pom.xml
├── Jenkinsfile
└── README.md
```

---

# 7. Create Java Maven Project

On your local Windows machine:

```powershell
mkdir sonarqube-jenkins-project
cd sonarqube-jenkins-project
```

Create the Maven project.

You can also use an existing Java Maven project.

Your `pom.xml` should contain something similar to:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>sonarqube-jenkins-project</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>

        </plugins>
    </build>

</project>
```

---

# 8. Java Application

Create:

```text
src/main/java/com/example/App.java
```

Example:

```java
package com.example;

public class App {

    public static void main(String[] args) {
        System.out.println("SonarQube Jenkins Project");
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }
}
```

---

# 9. Unit Test

Create:

```text
src/test/java/com/example/AppTest.java
```

```java
package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void testAddition() {
        assertEquals(10, App.add(5, 5));
    }

    @Test
    public void testMultiplication() {
        assertEquals(25, App.multiply(5, 5));
    }
}
```

---

# 10. Push Project to GitHub

```powershell
git init
git add .
git commit -m "Initial Java Maven SonarQube project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/sonarqube-jenkins-project.git
git push -u origin main
```

Verify:

```powershell
git remote -v
```

---

# 11. Install Jenkins Server

SSH into Jenkins EC2:

```bash
ssh -i your-key.pem ec2-user@JENKINS_PUBLIC_IP
```

Update:

```bash
sudo dnf update -y
```

Install Git:

```bash
sudo dnf install git -y
```

Install Java:

```bash
sudo dnf install java-21-amazon-corretto-devel -y
```

Verify:

```bash
java -version
```

You should see Java 21.

---

# 12. Install Maven

```bash
sudo dnf install maven -y
```

Verify:

```bash
mvn -version
```

You should see:

```text
Apache Maven
Java version: 21
```

---

# 13. Install Jenkins

Add the Jenkins repository:

```bash
sudo wget -O /etc/yum.repos.d/jenkins.repo \
https://pkg.jenkins.io/redhat-stable/jenkins.repo
```

Import key:

```bash
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2026.key
```

Install:

```bash
sudo dnf install jenkins -y
```

Enable:

```bash
sudo systemctl enable jenkins
```

Start:

```bash
sudo systemctl start jenkins
```

Check:

```bash
sudo systemctl status jenkins
```

Get password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Open:

```text
http://JENKINS_PUBLIC_IP:8080
```

---

# 14. Configure Jenkins

Complete:

```text
Unlock Jenkins
       ↓
Install suggested plugins
       ↓
Create admin user
       ↓
Jenkins Dashboard
```

Install/check these plugins:

```text
Git
GitHub
Pipeline
Pipeline: Stage View
SonarQube Scanner
Credentials Binding
Maven Integration
JUnit
```

---

# 15. Configure Java in Jenkins

Go to:

```text
Manage Jenkins
→ Tools
```

Find:

```text
JDK installations
```

Add:

```text
Name: Java21
```

If Java is installed on the server, use its path.

Check:

```bash
which java
```

and:

```bash
readlink -f $(which java)
```

Typical path:

```text
/usr/lib/jvm/java-21-amazon-corretto
```

---

# 16. Configure Maven

In:

```text
Manage Jenkins
→ Tools
→ Maven installations
```

Add:

```text
Name: Maven
```

You can select:

```text
Install automatically
```

or use the installed Maven.

---

# 17. Install SonarQube

Now SSH into the second EC2 instance:

```bash
ssh -i your-key.pem ec2-user@SONARQUBE_PUBLIC_IP
```

Update:

```bash
sudo dnf update -y
```

---

# 18. Install Java

```bash
sudo dnf install java-21-amazon-corretto-devel -y
```

Check:

```bash
java -version
```

---

# 19. Install PostgreSQL

For the SonarQube database:

```bash
sudo dnf install postgresql17-server postgresql17 -y
```

Initialize:

```bash
sudo postgresql-setup --initdb
```

Enable:

```bash
sudo systemctl enable postgresql
```

Start:

```bash
sudo systemctl start postgresql
```

Check:

```bash
sudo systemctl status postgresql
```

---

# 20. Create SonarQube Database

Switch to PostgreSQL user:

```bash
sudo -u postgres psql
```

Create user:

```sql
CREATE USER sonar WITH PASSWORD 'StrongPassword123';
```

Create database:

```sql
CREATE DATABASE sonarqube OWNER sonar;
```

Grant:

```sql
GRANT ALL PRIVILEGES ON DATABASE sonarqube TO sonar;
```

Exit:

```sql
\q
```

---

# 21. Test PostgreSQL

```bash
psql -U sonar -d sonarqube -h localhost
```

If you get:

```text
Ident authentication failed
```

edit:

```bash
sudo vi /var/lib/pgsql/17/data/pg_hba.conf
```

Find local authentication and change the appropriate line from:

```text
ident
```

to:

```text
md5
```

Restart:

```bash
sudo systemctl restart postgresql
```

Then:

```bash
psql -U sonar -d sonarqube -h localhost
```

Enter your password.

---

# 22. Install SonarQube

Create directory:

```bash
cd /opt
```

Download your chosen SonarQube version.

For example:

```bash
sudo wget <SONARQUBE_DOWNLOAD_URL>
```

Extract:

```bash
sudo unzip <sonarqube-file>.zip
```

Rename:

```bash
sudo mv <sonarqube-directory> sonarqube
```

Create user:

```bash
sudo useradd sonar
```

Change ownership:

```bash
sudo chown -R sonar:sonar /opt/sonarqube
```

---

# 23. Configure SonarQube Database

Edit:

```bash
sudo vi /opt/sonarqube/conf/sonar.properties
```

Configure:

```properties
sonar.jdbc.username=sonar
sonar.jdbc.password=StrongPassword123
sonar.jdbc.url=jdbc:postgresql://localhost:5432/sonarqube
```

For remote access to SonarQube, configure the server binding appropriately, for example:

```properties
sonar.web.host=0.0.0.0
```

---

# 24. Create SonarQube Systemd Service

```bash
sudo vi /etc/systemd/system/sonarqube.service
```

Use:

```ini
[Unit]
Description=SonarQube service
After=network.target postgresql.service

[Service]
Type=forking
User=sonar
Group=sonar

ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop

Restart=always
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

Reload:

```bash
sudo systemctl daemon-reload
```

Enable:

```bash
sudo systemctl enable sonarqube
```

Start:

```bash
sudo systemctl start sonarqube
```

Check:

```bash
sudo systemctl status sonarqube
```

---

# 25. Check SonarQube Logs

If it doesn't start:

```bash
sudo tail -f /opt/sonarqube/logs/sonar.log
```

Also:

```bash
sudo tail -f /opt/sonarqube/logs/web.log
```

And:

```bash
sudo tail -f /opt/sonarqube/logs/es.log
```

Check process:

```bash
ps -ef | grep sonar
```

Check port:

```bash
ss -tulpn | grep 9000
```

---

# 26. Open SonarQube

Browser:

```text
http://SONARQUBE_PUBLIC_IP:9000
```

Default credentials:

```text
Username: admin
Password: admin
```

SonarQube will ask you to change the password.

---

# 27. Install SonarScanner CLI

On SonarQube server:

```bash
cd /opt
```

Download:

```bash
sudo wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-8.1.0.6389.zip
```

Extract:

```bash
sudo unzip sonar-scanner-cli-8.1.0.6389.zip
```

Rename:

```bash
sudo mv sonar-scanner-8.1.0.6389 sonar-scanner
```

Add PATH:

```bash
echo 'export PATH=$PATH:/opt/sonar-scanner/bin' | sudo tee /etc/profile.d/sonar-scanner.sh
```

Load:

```bash
source /etc/profile.d/sonar-scanner.sh
```

Test:

```bash
sonar-scanner -h
```

Then:

```bash
sonar-scanner -v
```

---

# 28. Create SonarQube Project

Go to:

```text
SonarQube
→ Projects
→ Create Project
```

Use:

```text
Project display name:
sonarqube-jenkins-project

Project key:
sonarqube-jenkins-project
```

---

# 29. Create SonarQube Token

Go to:

```text
My Account
→ Security
→ Generate Tokens
```

For example:

```text
Name:
jenkins-sonarqube-token
```

Generate.

You'll get something like:

```text
sqp_xxxxxxxxxxxxxxxxx
```

### Important

Copy it immediately and store it securely.

Do **not** put it in:

```text
Jenkinsfile
GitHub
README.md
source code
shell history
```

---

# 30. Test Manual SonarQube Scan

Clone your project on SonarQube server:

```bash
cd ~
git clone https://github.com/YOUR_USERNAME/sonarqube-jenkins-project.git
```

Enter:

```bash
cd sonarqube-jenkins-project
```

Run:

```bash
sonar-scanner \
-Dsonar.projectKey=sonarqube-jenkins-project \
-Dsonar.sources=. \
-Dsonar.host.url=http://SONARQUBE_PRIVATE_OR_PUBLIC_IP:9000 \
-Dsonar.token=YOUR_NEW_TOKEN
```

Use the **new token**, not the exposed token from your original notes.

---

# 31. Verify Manual Scan

You should see something similar to:

```text
EXECUTION SUCCESS

ANALYSIS SUCCESSFUL
```

Then open:

```text
http://SONARQUBE_PUBLIC_IP:9000
```

Go to:

```text
Projects
→ sonarqube-jenkins-project
```

You should see:

```text
Bugs
Vulnerabilities
Code Smells
Security Hotspots
Coverage
Quality Gate
```

---

# 32. Connect Jenkins to SonarQube

Go to Jenkins:

```text
Manage Jenkins
→ Credentials
→ System
→ Global credentials
```

Create:

```text
Kind:
Secret text

Secret:
YOUR_NEW_SONAR_TOKEN

ID:
sonar-token

Description:
SonarQube Authentication Token
```

---

# 33. Configure SonarQube Server

Go to:

```text
Manage Jenkins
→ System
```

Find:

```text
SonarQube servers
```

Add:

```text
Name:
MySonar

Server URL:
http://SONARQUBE_PUBLIC_IP:9000
```

Authentication token:

```text
sonar-token
```

Save.

---

# 34. Configure SonarQube Scanner

Go to:

```text
Manage Jenkins
→ Tools
```

Find:

```text
SonarQube Scanner installations
```

Add:

```text
Name:
mySonar
```

Select:

```text
Install automatically
```

Save.

---

# 35. Create Jenkins Pipeline

Go to:

```text
Jenkins Dashboard
→ New Item
```

Name:

```text
sonarqube-jenkins-pipeline
```

Select:

```text
Pipeline
```

---

# 36. Jenkinsfile

Put this file in your GitHub repository:

```groovy
pipeline {

    agent any

    tools {
        jdk 'Java21'
        maven 'Maven'
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/YOUR_USERNAME/sonarqube-jenkins-project.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('SonarQube Analysis') {
            steps {

                script {

                    def scannerHome = tool 'mySonar'

                    withSonarQubeEnv('MySonar') {

                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=sonarqube-jenkins-project \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.java.binaries=target/classes
                        """
                    }
                }
            }
        }
    }

    post {

        success {
            echo 'Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed!'
        }

        always {
            junit 'target/surefire-reports/*.xml'
        }
    }
}
```

---

# 37. Configure Pipeline from GitHub

In Jenkins:

```text
Pipeline
→ Definition
→ Pipeline script from SCM
```

Select:

```text
SCM:
Git
```

Repository:

```text
https://github.com/YOUR_USERNAME/sonarqube-jenkins-project.git
```

Branch:

```text
*/main
```

Script Path:

```text
Jenkinsfile
```

Save.

---

# 38. Run Pipeline

Click:

```text
Build Now
```

Expected flow:

```text
Checkout
   ↓
Build
   ↓
Test
   ↓
SonarQube Analysis
   ↓
Success
```

---

# 39. Expected Jenkins Console

You should eventually see:

```text
[Pipeline] stage
[Pipeline] { (Checkout)

Checking out Revision ...

[Pipeline] stage
[Pipeline] { (Build)

mvn clean package -DskipTests

BUILD SUCCESS

[Pipeline] stage
[Pipeline] { (Test)

mvn test

Tests run: 2
Failures: 0
Errors: 0

[Pipeline] stage
[Pipeline] { (SonarQube Analysis)

ANALYSIS SUCCESSFUL

Finished: SUCCESS
```

---

# 40. GitHub → Jenkins Automatic Trigger

After manual pipeline execution works, configure automatic triggering.

Jenkins:

```text
Job
→ Configure
→ Build Triggers
```

Select:

```text
GitHub hook trigger for GITScm polling
```

Then configure GitHub webhook:

```text
GitHub Repository
→ Settings
→ Webhooks
→ Add webhook
```

Payload URL:

```text
http://JENKINS_PUBLIC_IP:8080/github-webhook/
```

Content type:

```text
application/json
```

Select:

```text
Just the push event
```

Save.

---

# 41. Complete CI Flow

Now the final workflow becomes:

```text
Developer
   │
   │ git push
   ▼
GitHub
   │
   │ Webhook
   ▼
Jenkins
   │
   ├── Checkout
   │
   ├── Maven Build
   │
   ├── Unit Tests
   │
   └── SonarScanner
            │
            ▼
       SonarQube
            │
            ├── Bugs
            ├── Vulnerabilities
            ├── Code Smells
            ├── Security
            └── Quality Gate
```

---

# 42. Recommended Final Project Structure

Your GitHub repository should look like:

```text
sonarqube-jenkins-project
│
├── src
│   ├── main
│   │   └── java
│   │       └── com
│   │           └── example
│   │               └── App.java
│   │
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── AppTest.java
│
├── Jenkinsfile
├── pom.xml
└── README.md
```

---

# 43. What You Should Demonstrate

For your assignment/demo, show these in order:

### Step 1 — AWS

Show:

```text
Jenkins EC2
SonarQube EC2
Security Groups
Public IPs
```

### Step 2 — Jenkins

Show:

```text
Jenkins Dashboard
Manage Jenkins → Tools
Java
Maven
SonarQube Scanner
```

### Step 3 — SonarQube

Show:

```text
SonarQube Dashboard
Project
Token configuration
Quality Gate
```

### Step 4 — GitHub

Show:

```text
Repository
pom.xml
Jenkinsfile
Java source
```

### Step 5 — Jenkins Pipeline

Run:

```text
Build Now
```

Show:

```text
Checkout
Build
Test
SonarQube Analysis
SUCCESS
```

### Step 6 — SonarQube Results

Refresh:

```text
SonarQube
→ Project
```

Show:

```text
Bugs
Vulnerabilities
Code Smells
Security Hotspots
Quality Gate
```

---

# 44. Troubleshooting Commands

### Jenkins

```bash
sudo systemctl status jenkins
sudo journalctl -u jenkins -f
```

Check port:

```bash
sudo ss -tulpn | grep 8080
```

---

### SonarQube

```bash
sudo systemctl status sonarqube
sudo journalctl -u sonarqube -f
```

```bash
ps -ef | grep sonar
```

```bash
ss -tulpn | grep 9000
```

Logs:

```bash
sudo tail -f /opt/sonarqube/logs/sonar.log
```

---

### PostgreSQL

```bash
sudo systemctl status postgresql
```

Test:

```bash
psql -U sonar -d sonarqube -h localhost
```

---

### Network Test from Jenkins

From Jenkins EC2:

```bash
curl http://SONARQUBE_IP:9000
```

If this works, Jenkins can reach SonarQube.

---

### Test Scanner

```bash
sonar-scanner -v
```

---

# 45. One Important Improvement to Your Original Assignment

Your original instructions put:

```text
sonar-scanner
```

on the **SonarQube server** and manually execute it there.

For the **Jenkins CI/CD architecture**, the better design is:

```text
GitHub
   ↓
Jenkins
   ↓
SonarScanner
   ↓
SonarQube
```

The scanner does **not** need to run on the SonarQube server.

So your production-style architecture should be:

```text
                 GitHub
                   │
                   ▼
              ┌─────────┐
              │ Jenkins │
              │         │
              │ Maven   │
              │ Scanner │
              └────┬────┘
                   │
                   │ HTTP 9000
                   ▼
             ┌───────────┐
             │ SonarQube │
             │           │
             │ PostgreSQL│
             └───────────┘
```

You can still install SonarScanner manually on the SonarQube EC2 for **Step 1/manual testing**, but Jenkins should perform the actual CI scan.

## Final deliverable

Your completed project will therefore demonstrate:

**AWS EC2 → GitHub → Jenkins → Maven Build → Unit Tests → SonarScanner → SonarQube → Quality Gate**

and will be a much closer **real-world CI code-quality pipeline** than simply running `sonar-scanner` manually on the SonarQube server.
