import sbt.Keys.{resolvers, _}
import sbt._

val appVersion = "0.1"
val scalaJvmVersion = "2.11.12"
// library versions
val akkaVersion = "2.5.9"
val awsVersion = "1.11.631"
val confluentVersion = "5.1.0"
val hadoopVersion = "2.9.0"
val jacksonVersion = "2.9.1"
val protobufVersion = "2.5.0"
val scalaTestVersion = "3.0.1"
val slf4jVersion = "1.7.25"
val sparkVersion = "2.4.2"

///////////////////////////////////////////////////////////////////
//    Dependency Functions
///////////////////////////////////////////////////////////////////

lazy val actorSettings = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

lazy val appSettings = Seq(
  "log4j" % "log4j" % "1.2.17" % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test
)

lazy val awsSettings = Seq(
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-sns" % awsVersion
)

lazy val dynamoSettings = Seq(
  "com.gu" %% "scanamo" % "1.0.0-M8"
)

lazy val kafkaAvro = Seq(
  "io.confluent" % "kafka-avro-serializer" % confluentVersion
)

lazy val kinesisSettings = Seq(
  "com.amazonaws" % "aws-java-sdk-kinesis" % awsVersion
)

lazy val loggingSettings = Seq(
  "log4j" % "log4j" % "1.2.17",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-log4j12" % slf4jVersion
)

lazy val resolverSettings = Seq(
  "confluent" at "http://packages.confluent.io/maven/",
  DefaultMavenRepository,
  "spark-packages" at "https://dl.bintray.com/spark-packages/maven/",
  "DynamoDB Local Release Repository" at "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"
)

lazy val sageMakerSettings = Seq(
  "com.amazonaws" % "aws-java-sdk-sagemaker" % awsVersion,
  "com.amazonaws" % "aws-java-sdk-sagemakerruntime" % awsVersion
)

def sparkAppSettings(provided: Boolean, includeStreamingLibs: Boolean = true): Seq[ModuleID] = {
  val scope = if (provided) Provided else Compile
  val sparkDependencyFixes = Seq(
    // Jackson
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,

    // Google Protocol Buffers
    "com.google.protobuf" % "protobuf-java" % protobufVersion,

    // Hadoop
    "org.apache.hadoop" % "hadoop-aws" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion,

    // Specifying a lower version of some compression
    // library that conflicts between spark 2.3 and kafka 0.10
    "net.jpountz.lz4" % "lz4" % "1.3.0"
  )

  val sparkStreamingLibs = Seq(
    "org.apache.spark" %% "spark-streaming" % sparkVersion,
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
    "org.apache.spark" %% "spark-streaming-kinesis-asl" % sparkVersion
  )

  val sparkLibs = Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % scope,
    "org.apache.spark" %% "spark-hive" % sparkVersion % scope,
    "org.apache.spark" %% "spark-sql" % sparkVersion % scope
  )

  // tie it all together
  if (includeStreamingLibs)
    sparkLibs ++ testDependencies ++ sparkStreamingLibs ++ sparkDependencyFixes
  else
    sparkLibs ++ sparkDependencyFixes ++ testDependencies
}

lazy val sparkAvro = Seq(
  "org.apache.spark" %% "spark-avro" % sparkVersion
)

lazy val testDependencies = Seq(
  "log4j" % "log4j" % "1.2.17" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion % Test,
  "com.amazonaws" % "DynamoDBLocal" % "latest.integration" % Test,
  "com.holdenkarau" %% "spark-testing-base" % "2.4.0_0.11.0" % Test,
  "tech.allegro.schema.json2avro" % "converter" % "0.2.5" % Test,
  "net.jpountz.lz4" % "lz4" % "1.3.0" % Test
)

///////////////////////////////////////////////////////////////////
//    Core projects
///////////////////////////////////////////////////////////////////

lazy val core = (project in file("./app/common/core"))
  .settings(
    name := "core",
    organization := "com.coxauto.mediaanalytics",
    description := "Core",
    version := appVersion,
    homepage := Some(url("https://ghe.coxautoinc.com/EPD-Analytics/vehicle-score-v3")),
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= awsSettings ++ testDependencies ++ loggingSettings ++ Seq(
      "commons-io" % "commons-io" % "2.6",
      "net.liftweb" %% "lift-json" % "3.1.1"
    ))


lazy val hadoop_file_rename = (project in file("./app/common/hadoop-utils"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "hadoop-file-rename",
    organization := "com.coxauto.mediaanalytics",
    description := "Hadoop file rename application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.hadoop.FileRename"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++=
      Seq("org.apache.hadoop" % "hadoop-client" % hadoopVersion,
        "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
        "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion
      ))

lazy val common_aws = (project in file("./app/common/aws"))
  .dependsOn(core)
  .settings(
    name := "common-aws",
    organization := "com.coxauto.mediaanalytics",
    description := "AWS Commons",
    version := appVersion,
    homepage := Some(url("https://ghe.coxautoinc.com/EPD-Analytics/vehicle-score-v3")),
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= actorSettings ++ awsSettings ++ testDependencies ++ kinesisSettings ++ loggingSettings)

lazy val common_spark = (project in file("./app/common/spark"))
  .dependsOn(core)
  .settings(
    name := "common-spark",
    organization := "com.coxauto.mediaanalytics",
    description := "Spark Commons",
    version := appVersion,
    homepage := Some(url("https://ghe.coxautoinc.com/EPD-Analytics/vehicle-score-v3")),
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = true, includeStreamingLibs = false) ++ Seq(
      "org.apache.commons" % "commons-dbcp2" % "2.0.1",
      "com.h2database" % "h2" % "1.4.192" % Test,
      "mysql" % "mysql-connector-java" % "5.1.24"
    ))


lazy val chrome_table_copy = (project in file("./app/vehicle-scores-v3/chrome-table-copy"))
  .dependsOn(common_aws, common_spark, vehicle_scores_v3_common)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "chrome-table-copy",
    organization := "com.coxauto.mediaanalytics",
    description := "Chrome Table Copy",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false, includeStreamingLibs = false) ++ loggingSettings ++ Seq(
      "commons-net" % "commons-net" % "3.6",
      "com.audienceproject" %% "spark-dynamodb" % "1.0.2",
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    ))

/**
  * Common Test Utils
  * @example sbt "project test_utils" test
  */
lazy val test_utils = (project in file("./app/common/test-utils"))
  .dependsOn(core, common_aws)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "test-utils",
    organization := "com.coxauto.mediaanalytics",
    description := "Test Utilities",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= loggingSettings)

///////////////////////////////////////////////////////////////////
//   Pixall Merge projects
///////////////////////////////////////////////////////////////////

/**
  * PixAll Merge: Common
  * @example sbt "project pixall_common" test
  */
lazy val pixall_common = (project in file("./app/common/pixall"))
  .aggregate(core, common_aws, common_spark)
  .dependsOn(core, common_aws, common_spark)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "pixall-common",
    organization := "com.coxauto.mediaanalytics",
    description := "PixAll Common",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = true, includeStreamingLibs = true) ++ kafkaAvro)

///////////////////////////////////////////////////////////////////
//   Vehicle Scores V3 projects (Streaming)
///////////////////////////////////////////////////////////////////

/**
  * Vehicle Scores v3 common
  * @example sbt "project vehicle_scores_v3_common" clean compile
  */
lazy val vehicle_scores_v3_common = (project in file("./app/vehicle-scores-v3/common"))
  .dependsOn(common_aws, common_spark, vehicle_score_service_v3, pixall_common)
  .settings(
    name := "vehicle-score-common",
    organization := "com.coxauto.mediaanalytics",
    description := "Vehicle Score Commons",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= resolverSettings,
    test in assembly := {},
    libraryDependencies ++= dynamoSettings ++ loggingSettings ++ sageMakerSettings ++ sparkAppSettings(provided = true) ++ Seq(
      "org.apache.commons" % "commons-dbcp2" % "2.0.1" ,
      "com.jcabi" % "jcabi-manifests" % "1.1"
    ))

/**
  * Entity Account Ingest
  * @example sbt "project entity_account_ingest" clean run
  * @example sbt "project entity_account_ingest" clean universal:packageBin
  */
lazy val entity_account_ingest = (project in file("./app/vehicle-scores-v3/entity-account"))
  .dependsOn(vehicle_scores_v3_common, common_aws, pixall_common, common_spark)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "entity-account-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "Entity Account Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.account.EntityAccountSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false, includeStreamingLibs = false) ++ loggingSettings ++ Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    ))

/**
  * Dealer Code to Entity Account Mapping
  * @example sbt "project account_mapping_ingest" clean run
  * @example sbt "project account_mapping_ingest" clean universal:packageBin
  */
lazy val account_mapping_ingest = (project in file("./app/vehicle-scores-v3/account-mapping"))
  .dependsOn(vehicle_scores_v3_common, common_aws, pixall_common, common_spark)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "account-mapping-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "Dealer to Entity Account Mapping Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.account_mapping.DealerCodeToAccountIdSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false, includeStreamingLibs = false)
      ++ loggingSettings ++ sparkAvro ++ Seq("net.jpountz.lz4" % "lz4" % "1.3.0")
  )

/**
  * Nielsen Zip Code Ingest
  * @example sbt "project zip_code_ingest" clean run
  * @example sbt "project zip_code_ingest" clean universal:packageBin
  */
lazy val zip_code_ingest = (project in file("./app/vehicle-scores-v3/zipcode-state"))
  .dependsOn(vehicle_scores_v3_common, common_aws, pixall_common, common_spark)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "zip-code-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "Zip Code Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.zipcode.ZipCodeStateMappingSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false, includeStreamingLibs = false)
      ++ loggingSettings ++ Seq("net.jpountz.lz4" % "lz4" % "1.3.0")
  )

/**
  * ATC Listing Ingest
  * @example sbt "project atc_listing_ingest" clean run
  * @example sbt "project atc_listing_ingest" clean universal:packageBin
  */
lazy val atc_listing_ingest = (project in file("./app/vehicle-scores-v3/atc-inventory-listings"))
  .dependsOn(common_aws, pixall_common, common_spark, vehicle_scores_v3_common)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "atc-listing-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "ATC Listing Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.listing.AtcInventoryListingSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = true, includeStreamingLibs = false) ++ loggingSettings ++ sparkAvro ++ Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    ))

/**
  * DDC Inventory Ingest
  * @example sbt "project ddc_inventory_ingest" clean run
  * @example sbt "project ddc_inventory_ingest" clean universal:packageBin
  */
lazy val ddc_inventory_ingest = (project in file("./app/vehicle-scores-v3/ddc-inventory"))
  .dependsOn(common_aws, pixall_common, common_spark, vehicle_scores_v3_common)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "ddc-inventory-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "DDC Inventory Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.listing.DdcInventorySparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = true, includeStreamingLibs = false)
      ++ loggingSettings ++ sparkAvro ++ Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    ))


///////////////////////////////////////////////////////////////////
//    YMM Market Summary Ingest project
///////////////////////////////////////////////////////////////////

/**
  * Vehicle Market Summary Ingest
  * @example sbt "project vehicle_market_summary_ingest" clean run
  * @example sbt "project vehicle_market_summary_ingest" clean universal:packageBin
  * @example sbt "project vehicle_market_summary_ingest" clean assembly
  */
lazy val vehicle_market_summary_ingest = (project in file("./app/vehicle-scores-v3/market-summary"))
  .dependsOn(pixall_common, vehicle_scores_v3_common, common_aws, common_spark)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vehicle-market-summary-ingest",
    organization := "com.coxauto.mediaanalytics",
    description := "YMM Market Viewer Counts Ingest Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.market.MarketDistinctViewerCountSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false, includeStreamingLibs = false)
      ++ loggingSettings ++ sparkAvro ++ Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    ))

lazy val inactive_scores = (project in file("./app/vehicle-scores-v3/inactive"))
  .dependsOn(common_aws)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "inactive-scores",
    organization := "com.coxauto.mediaanalytics",
    description := "Inactive Scoring Lambda",
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.inactive.InactiveScores"),
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some(name.value),
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.rename
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case PathList("org", "apache", "spark", _*) => MergeStrategy.discard
      case PathList("akka", _*) => MergeStrategy.discard
      case PathList("com", "gu",  _*) => MergeStrategy.discard
      case PathList("com", "sun", "jersey",  _*) => MergeStrategy.discard
      case PathList("jersey",  _*) => MergeStrategy.discard
      case PathList("org", "apache", "hadoop",  _*) => MergeStrategy.discard
      case PathList("org", "apache", "kafka",  _*) => MergeStrategy.discard
      case PathList("org", "apache", "avro",  _*) => MergeStrategy.discard
      case PathList("org", "glassfish",  _*) => MergeStrategy.discard
      case PathList("org", "jboss",  _*) => MergeStrategy.discard
      case PathList("org", "mortbay",  _*) => MergeStrategy.discard
      case PathList("org", "spark_project",  _*) => MergeStrategy.discard
      case PathList("io", "confluent", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","dynamodbv2", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","sagemakerruntime", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","glue", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","glacier", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","elasticbeanstalk", _*) => MergeStrategy.discard
      case PathList("com","amazonaws","services","elasticmapreduce", _*) => MergeStrategy.discard
      case PathList("com","ctc", _*) => MergeStrategy.discard
      case PathList("win", _*) => MergeStrategy.discard
      case PathList("win32", _*) => MergeStrategy.discard
      case PathList("webapps", _*) => MergeStrategy.discard
      case PathList("com","microsoft", _*) => MergeStrategy.discard
      case PathList("avro", _*) => MergeStrategy.discard
      case PathList("shapeless", _*) => MergeStrategy.discard
      case PathList("cats", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,

    libraryDependencies ++=
       loggingSettings ++ Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
      "com.amazonaws" % "aws-lambda-java-events" % "2.2.5",
      "mysql" % "mysql-connector-java" % "5.1.24"
    ))

/**
  * Streaming Vehicle Web Activity V3
  * @example sbt "project vehicle_web_activity_streaming_v3" clean run
  * @example sbt "project vehicle_web_activity_streaming_v3" clean universal:packageBin
  */
lazy val vehicle_web_activity_streaming_v3 = (project in file("./app/vehicle-scores-v3/web-activity-streaming"))
  .dependsOn(vehicle_scores_v3_common, pixall_common, test_utils)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vehicle-web-activity-streaming-v3",
    organization := "com.coxauto.mediaanalytics",
    description := "Streaming Vehicle Scores V3 Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some("streaming-scores-v3"),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.v3.streaming.VehicleWebActivityStreamingSparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.first
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false) ++ actorSettings ++ kafkaAvro ++ loggingSettings ++ sparkAvro
  )

/**
  * Streaming Vehicle Historical Web Activity V3
  * @example sbt "project vehicle_historical_web_activity_v3" clean run
  * @example sbt "project vehicle_historical_web_activity_v3" clean universal:packageBin
  */
lazy val vehicle_historical_web_activity_v3 = (project in file("./app/vehicle-scores-v3/historical-web-activity"))
  .dependsOn(vehicle_scores_v3_common, pixall_common, test_utils)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vehicle-historical-web-activity-v3",
    organization := "com.coxauto.mediaanalytics",
    description := "Historical Web Activity V3 Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some("streaming-scores-v3"),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.v3.streaming.HistoricalWebActivitySparkJob"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.first
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false) ++ actorSettings ++ kafkaAvro ++ loggingSettings ++ sparkAvro)

/**
  * Merchandising Summary Spark Job
  * @example sbt "project vehicle_merchandising_summary_v3" clean run
  * @example sbt "project vehicle_merchandising_summary_v3" clean universal:packageBin
  */
lazy val vehicle_merchandising_summary_v3 = (project in file("./app/vehicle-scores-v3/merchandising-summary"))
  .dependsOn(vehicle_scores_v3_common, pixall_common, test_utils)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vehicle-merchandising-summary",
    organization := "com.coxauto.mediaanalytics",
    description := "Merchandising Summary V3 Spark application",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some("merchandising-summary"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.first
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = true, includeStreamingLibs = false)
      ++ loggingSettings ++ Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0"
    )
  )

/**
  * Vehicle Score Service V3
  * @example sbt "project vehicle_scoring_v3" clean assembly
  * @example sbt "project vehicle_scoring_v3" clean run
  */
lazy val vehicle_scoring_v3 = (project in file("./app/vehicle-scores-v3/scoring"))
  .dependsOn(vehicle_scores_v3_common, pixall_common, vehicle_score_service_v3)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GitVersioning)
  .settings(
    name := "vehicle-scoring-v3",
    organization := "com.coxauto.mediaanalytics",
    description := "Streaming Vehicle Scores V3 Spark application",
    version := appVersion,
    git.useGitDescribe := true,
    git.gitTagToVersionNumber := { tag: String =>
      val pattern = "\\d.\\d{4}.\\d{2}.\\d{2}.\\d".r
      val releaseVersion = pattern.findFirstIn(tag)
      if(releaseVersion.isDefined) releaseVersion
      else Some("Untagged")
    },
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some("streaming-scores-v3"),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.v3.streaming.VehicleScoringSparkJob"),
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes( "Release-Version" -> s"${git.gitDescribedVersion.value.getOrElse("Untagged")}" ),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.first
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false) ++ loggingSettings,
)

/**
  * Vehicle Score Replayer V3
  * @example sbt "project vehicle_score_replayer_v3" clean assembly
  * @example sbt "project vehicle_score_replayer_v3" clean run
  */
lazy val vehicle_score_replayer_v3 = (project in file("./app/vehicle-scores-v3/replayer"))
  .dependsOn(vehicle_scoring_v3)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "vehicle-score-replayer-v3",
    organization := "com.coxauto.mediaanalytics",
    description := "Vehicle Score Player v3 testing tool",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    topLevelDirectory := Some("streaming-scores-v3"),
    mainClass in assembly := Some("com.coxautoinc.mediaanalytics.v3.replayer.VehicleScoreReplayer"),
    test in assembly := {},
    assemblyJarName in assembly := s"${name.value}-${version.value}.fat.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
      case PathList("META-INF", "MANIFEST.MF", _*) => MergeStrategy.discard
      case PathList("META-INF", _*) => MergeStrategy.filterDistinctLines
      case PathList("org", "datanucleus", _*) => MergeStrategy.first
      case PathList("com", "scoverage", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    resolvers ++= resolverSettings,
    libraryDependencies ++= sparkAppSettings(provided = false) ++ loggingSettings)

/**
  * Vehicle Score Service V3
  * @example sbt "project vehicle_score_service_v3" clean compile
  */
lazy val vehicle_score_service_v3 = (project in file("./app/vehicle-scores-v3/service"))
  .dependsOn(common_aws)
  .settings(
    name := "vehicle-score-service-v3",
    organization := "com.coxauto.mediaanalytics",
    description := "Vehicle Score Service V3",
    version := appVersion,
    scalaVersion := scalaJvmVersion,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions", "-Xlint"),
    scalacOptions in(Compile, doc) ++= Seq("-no-link-warnings"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= resolverSettings,
    libraryDependencies ++= appSettings ++ loggingSettings ++ sageMakerSettings)

///////////////////////////////////////////////////////////////////
//   DynamoDB Test Stuff
///////////////////////////////////////////////////////////////////

// Native Libs
libraryDependencies += "com.almworks.sqlite4java" % "sqlite4java" % "latest.integration" % "test"
libraryDependencies += "com.almworks.sqlite4java" % "sqlite4java-win32-x86" % "latest.integration" % "test"
libraryDependencies += "com.almworks.sqlite4java" % "sqlite4java-win32-x64" % "latest.integration" % "test"
libraryDependencies += "com.almworks.sqlite4java" % "libsqlite4java-osx" % "latest.integration" % "test"
libraryDependencies += "com.almworks.sqlite4java" % "libsqlite4java-linux-i386" % "latest.integration" % "test"
libraryDependencies += "com.almworks.sqlite4java" % "libsqlite4java-linux-amd64" % "latest.integration" % "test"

// Run 'copyJars' before compile
lazy val copyJars = taskKey[Unit]("copyJars")
copyJars := {
  import java.io.File
  import java.nio.file.Files
  // For Local Dynamo DB to work, we need to copy SQLite native libs from
  // our test dependencies into a directory that Java can find ("native-libs" in this case)
  // Then in our Java/Scala program, we need to set System.setProperty("sqlite4java.library.path", "native-libs");
  // before attempting to instantiate a DynamoDBEmbedded instance
  val artifactTypes = Set("dylib", "so", "dll")
  val files = Classpaths.managedJars(Test, artifactTypes, update.value).files
  Files.createDirectories(new File(baseDirectory.value, "native-libs").toPath)
  files.filterNot(_.exists()).foreach { f =>
    val fileToCopy = new File("native-libs", f.name)
    Files.copy(f.toPath, fileToCopy.toPath)
  }
}

(compile in Compile) := (compile in Compile).dependsOn(copyJars).value

// ScalaTest
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
