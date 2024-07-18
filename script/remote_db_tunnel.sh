#!/bin/bash

trap "pkill -f 'port-forward'" EXIT

NAMESPACE=
PORT=
RDS_SECRET_NAME=
PORT_FORWARD_CONTAINER_NAME=
IMAGE=
DISPLAY_CREDS=0
CLI=0
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

while getopts "dcp:s:n:i:" flag; do
    case "$flag" in
    c)
        CLI=1;;
    p)
        PORT=${OPTARG}
        ;;
    s)
        RDS_SECRET_NAME=${OPTARG}
        ;;
    n)
        PORT_FORWARD_CONTAINER_NAME=${OPTARG}
        ;;
    i)
        IMAGE=${OPTARG}
        ;;
    d)
        DISPLAY_CREDS=1;;
    :)
        echo "Option -${OPTARG} requires an argument."
        exit 1
        ;;
    ?)
        echo "Invalid option: -${OPTARG}."
        exit 1
        ;;
    esac
done

shift $(($OPTIND - 1));
ENV=$1;

if [ -z "$ENV" ] || { [ "$ENV" != "dev" ] && [ "$ENV" != "test" ] && [ "$ENV" != "preprod" ]&& [ "$ENV" != "prod" ] ;} then
  echo "Name:"
  echo "    remote_db_tunnel - creates a port forwarding tunnel to connect to remote database"
  echo ""
  echo "Synopsis:"
  echo "    remote_db_tunnel [dev|test|preprod|prod] - use CAS_PORT_FORWARD_CONTAINER_NAME env var"
  echo "    remote_db_tunnel -n port-forward-pod-da [dev|test|preprod|prod] - define port forward pod name"
  echo "    remote_db_tunnel -c [dev|test|preprod|prod] - create SQL CLI connection once forwarding setup"
  echo "    remote_db_tunnel -d [dev|test|preprod|prod] - display credentials required to connect"
  echo ""
  echo "Description:"
  echo ""
  echo "    To connect to a remove database port forwarding must be used, as per guidance available from "
  echo "    https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html"
  echo ""
  echo "    This script will"
  echo ""
  echo "    1) Create a port forwarding pod"
  echo "    2) Setup local port forwarding"
  echo "    3) Optionally output connection details, including credentials"
  echo "    4) Optionally setup a CLI SQL Client (jaqy) automatically connecting to the POD"
  echo "    5) Destroy the port forwarding pod on completion"
  echo "    "
  echo "    It's recommended that an environment variable named 'CAS_PORT_FORWARD_CONTAINER_NAME' is"
  echo "    setup to provide the default port froward port name. This should be unique for each user"
  echo "    (e.g. port-forward-pod-{initials}) because each pod can only be used for one port forward"
  echo "    session at a time"
  echo ""
  echo "    The options are as follows. All options must precede the environment argument"
  echo ""
  echo "    -c    If a SQL CLI (jaqy) should be started once port forwarding is setup. Defaults to false"
  echo ""
  echo "    -d    If connection details include credentials should be shown. Defaults to false"
  echo ""
  echo "    -i    Image to use for the port forward container. Defaults to ministryofjustice/port-forward"
  echo ""
  echo "    -n    Name to use for the port forward container. If not defined, will use the CAS_PORT_FORWARD_CONTAINER_NAME env var. This must be unique for each user (they cannot be shared)"
  echo ""
  echo "    -p    Port used for local and remote forwarding. Defaults to 5432"
  echo ""
  echo "    -s    Name of secret used to retrieve RDS connection details. Defaults to rds-postgresql-instance-output"
  echo ""
  exit 1
fi

NAMESPACE="hmpps-community-accommodation-$ENV"
[ -z "${PORT}" ] && PORT=5432
[ -z "${RDS_SECRET_NAME}" ] && RDS_SECRET_NAME='rds-postgresql-instance-output' #most used name for the rds instance secret.
[ -z "${IMAGE}" ] && IMAGE='ministryofjustice/port-forward'

if [ -z "${PORT_FORWARD_CONTAINER_NAME}" ]
then
  if [ -z "${CAS_PORT_FORWARD_CONTAINER_NAME}" ]
  then
    PORT_FORWARD_CONTAINER_NAME='port-forward-pod'
  else
    PORT_FORWARD_CONTAINER_NAME=${CAS_PORT_FORWARD_CONTAINER_NAME}
  fi
fi

echo "---------------------------------------------------------------"
echo "The following parameters will be used:"
echo ""
echo "* Namespace: $NAMESPACE"
echo "* Port: $PORT"
echo "* RDS Secret Name: $RDS_SECRET_NAME"
echo "* Port Forward Container Name: $PORT_FORWARD_CONTAINER_NAME"
echo "* Port Forward Image: $IMAGE"
echo ""

# ensure we're in the right k8s context
kubectl config use-context live.cloud-platform.service.justice.gov.uk

secrets=$(kubectl get secrets $RDS_SECRET_NAME --namespace "$NAMESPACE" -o json | jq ".data | map_values(@base64d)")
rds_instance_endpoint=$(echo "$secrets" | jq -r '.rds_instance_address')
database_name=$(echo "$secrets" | jq -r '.database_name')
database_username=$(echo "$secrets" | jq -r '.database_username')
database_password=$(echo "$secrets" | jq -r '.database_password')

echo "---------------------------------------------------------------"
echo "Checking if port forward container already exists"
echo ""
kubectl get pod $PORT_FORWARD_CONTAINER_NAME -n "$NAMESPACE"
POD_EXISTS_RETURN_CODE=$?
echo ""

if [ $POD_EXISTS_RETURN_CODE -gt 0 ]
then
    echo "Creating pod..."
    kubectl -n "$NAMESPACE" run $PORT_FORWARD_CONTAINER_NAME --image=$IMAGE \
        --port=$PORT \
        --env="REMOTE_HOST=$rds_instance_endpoint" \
        --env="LOCAL_PORT=$PORT" \
        --env="REMOTE_PORT=$PORT"
    kubectl wait --for=condition=ready pod/$PORT_FORWARD_CONTAINER_NAME -n "$NAMESPACE"
    echo "Port-forward pod $PORT_FORWARD_CONTAINER_NAME created!"
else
    echo "Port-forward pod already exists"
fi

if [ "$DISPLAY_CREDS" -eq 1 ]
then
    echo ""
    echo "---------------------------------------------------------------"
    echo "Connect using the following details in your local Database IDE"
    echo ""
    echo "* Host: localhost"
    echo "* Database Name: $database_name"
    echo "* User: $database_username"
    echo "* Password: $database_password"
    echo "* Port: $PORT"
    echo ""
fi

if [ $CLI -eq 0 ]
then
    echo "Forwarding port. Use ctl-c to close port forwarding"
    kubectl -n "$NAMESPACE" port-forward $PORT_FORWARD_CONTAINER_NAME $PORT:$PORT
else
    # This is ran in the background so we can start the cli
    # we use trap at the top of this script to kill it on exit
    kubectl -n "$NAMESPACE" port-forward $PORT_FORWARD_CONTAINER_NAME $PORT:$PORT &

    port_forward_pid=$!
    echo "Port forward pid is $port_forward_pid"

    echo "Port forwarding setup. Now starting CLI"

    JAQY_CHECKSUM="6d426a6ccf8f7ff07c06346a07a2921561561aa8d9405dace115a6c1947bb4c4"
    JAQY_URL="https://github.com/Teradata/jaqy/releases/download/v1.2.0/jaqy-1.2.0.jar"
    POSTGRES_URL="https://jdbc.postgresql.org/download/postgresql-42.7.3.jar"

    app_dir=$(dirname $PWD)
    bin_dir=$app_dir/bin

    mkdir -p "$bin_dir"
    jaqy_path="$bin_dir/jaqy-1.2.0.jar"
    postgres_driver_path="$bin_dir/postgresql-42.7.3.jar"

    if [ ! -f "$jaqy_path" ]; then
        echo "Downloading jacqy to $jaqy_path"
       curl -L $JAQY_URL --output "$jaqy_path"
    fi

    calculated_sha=$(shasum -a 256 $jaqy_path | awk '{ print $1 }')
    echo ""
    if [[ "$calculated_sha" != "$JAQY_CHECKSUM" ]]; then
      echo "jaqy checksum failed"
      exit 1
    fi

    if [ ! -f "$postgres_driver_path" ]; then
       curl -L $POSTGRES_URL --output "$postgres_driver_path"
    fi

    echo ""
    echo "---------------------------------------------------------------"
    echo "Starting CLI connection to $ENV"
    echo ""

    java -jar "$jaqy_path" -- \
      .classpath postgresql "$postgres_driver_path" \; \
      .open -u "$database_username" -p "$database_password" postgresql://localhost:$PORT/"$database_name" \; \
      "SET default_transaction_read_only = TRUE ;"

    echo "Killing backgrounded port forward process $port_forward_pid"
    (kill -9 $port_forward_pid) 2>/dev/null
fi

echo "Deleting pod $PORT_FORWARD_CONTAINER_NAME. This may take a few seconds."
kubectl -n "$NAMESPACE" delete pod "$PORT_FORWARD_CONTAINER_NAME"
