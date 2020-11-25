# Prints usage instructions
function usage() {
        cat <<USAGE_END
Usage:   `basename $0` --src_jar <path> --src_url <url> --src_cred <user:pwd> --dest_jar <path> --dest_url <url> --dest_cred <user:pwd> [--enable_nodes]

Options:
  --src_jar <path>        Path to source Jenkins' CLI jar file
  --src_url <url>         URL to source Jenkins server
  --src_cred <user:pwd>   Credentials for authorised user on source Jenkins server
  --dest_jar <path>       Path to source Jenkins' CLI jar file
  --dest_url <url>        URL to source Jenkins server
  --dest_cred <user:pwd>  Credentials for authorised user on source Jenkins server
  --enable_nodes          Enable nodes to process jobs after migration
  --help                  Help (prints this usage information)
USAGE_END
}

# Define allowed arguments
TEMP=`getopt -o vdm: --long src_jar:,src_url:,src_cred:,dest_jar:,dest_url:,dest_cred:,enable_nodes,help \
             -n 'migrate-jobs' -- "$@"`

if [ $? != 0 ] ; then echo "Terminating..." >&2 ; exit 1 ; fi

eval set -- "$TEMP"

# Set variables from arguments
SRC_JAR=
SRC_URL=
SRC_CRED=
DEST_JAR=
DEST_URL=
DEST_CRED=
ENABLE_NODES=false
while true; do
  case "$1" in
    --src_jar ) SRC_JAR="$2"; shift 2 ;;
    --src_url ) SRC_URL="$2"; shift 2 ;;
    --src_cred ) SRC_CRED="$2"; shift 2 ;;
    --dest_jar ) DEST_JAR="$2"; shift 2 ;;
    --dest_url ) DEST_URL="$2"; shift 2 ;;
    --dest_cred ) DEST_CRED="$2"; shift 2 ;;
    --enable_nodes ) ENABLE_NODES=true; shift ;;
    --help ) usage; exit 0 ;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

# Arguments must be passed
if [[ -z $SRC_JAR || -z $SRC_URL || -z $SRC_CRED || -z $DEST_JAR || -z $DEST_URL || -z $DEST_CRED ]]; then
  echo 'All required arguments must be passed.'
  usage
  exit 1
fi

# Set token for splitting strings into arrays
IFS=$'\n'

# Don't allow more jobs to be processed on destination
echo "Stopping destination server from processing jobs"
nodejson=$(curl -sS $DEST_URL/computer/api/json?tree=computer[displayName] --user $DEST_CRED)
nodes=$(grep -Po '(?<="displayName":")[^"]*' <<< "$nodejson")
for node in $nodes; do
  echo "Stopping node $node"
  if [[ $node = 'master' ]]; then
    node=""
  fi
  java -jar $DEST_JAR -auth $DEST_CRED -s $DEST_URL -webSocket offline-node "$node" -m "migrating jobs"
done

# Find jobs
jobs=$(java -jar $SRC_JAR -auth $SRC_CRED -s $SRC_URL -webSocket list-jobs)
numJobs=$(echo "$jobs" | wc -l)
echo "Found $numJobs jobs."

# Copy jobs from source server to destination server
count=0
for job in $jobs; do
  count=$((count+1))
  echo "Migrating job '$job'"
  java -jar $SRC_JAR -auth $SRC_CRED -s $SRC_URL -webSocket get-job $job > /tmp/jenkins-job-config.xml
  java -jar $DEST_JAR -auth $DEST_CRED -s $DEST_URL -webSocket create-job "$job" < /tmp/jenkins-job-config.xml
done

# Allow destination nodes to start processing jobs again
if [ "$ENABLE_NODES" = true ]; then
  echo "Allowing nodes to begin processing jobs"
  for node in $nodes; do
    echo "Starting node $node"
    if [[ $node = 'master' ]]; then
      node=""
    fi
    java -jar $DEST_JAR -auth $DEST_CRED -s $DEST_URL -webSocket online-node "$node"
  done
fi

# Cleanup
rm /tmp/jenkins-job-config.xml
echo "Processed $count jobs."