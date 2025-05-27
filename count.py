#!/bin/bash

# MongoDB Vault Connection Script
# This script retrieves MongoDB credentials from HashiCorp Vault and connects using mongosh

set -e  # Exit on any error

# Configuration - Modify these variables according to your setup
VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-}"
VAULT_SECRET_PATH="${VAULT_SECRET_PATH:-secret/mongodb}"
MONGO_HOST="${MONGO_HOST:-localhost}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_DATABASE="${MONGO_DATABASE:-admin}"
MONGO_AUTH_SOURCE="${MONGO_AUTH_SOURCE:-admin}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if required tools are installed
check_dependencies() {
    print_status "Checking dependencies..."
    
    if ! command -v vault &> /dev/null; then
        print_error "HashiCorp Vault CLI is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v mongosh &> /dev/null; then
        print_error "mongosh is not installed or not in PATH"
        print_error "Please install MongoDB Shell: https://docs.mongodb.com/mongodb-shell/install/"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        print_error "jq is not installed or not in PATH"
        print_error "Please install jq for JSON parsing"
        exit 1
    fi
}

# Function to authenticate with Vault if needed
vault_auth() {
    print_status "Checking Vault authentication..."
    
    # Export VAULT_ADDR for vault commands
    export VAULT_ADDR
    
    # Check if already authenticated
    if vault auth -method=token &> /dev/null; then
        print_status "Already authenticated with Vault"
        return 0
    fi
    
    # If VAULT_TOKEN is set, use it
    if [ -n "$VAULT_TOKEN" ]; then
        export VAULT_TOKEN
        print_status "Using provided VAULT_TOKEN"
        return 0
    fi
    
    # Interactive authentication
    print_warning "Not authenticated with Vault. Please authenticate first."
    echo "You can:"
    echo "1. Set VAULT_TOKEN environment variable"
    echo "2. Run 'vault auth' manually"
    echo "3. Use other authentication methods"
    exit 1
}

# Function to retrieve credentials from Vault
get_credentials() {
    print_status "Retrieving MongoDB credentials from Vault path: $VAULT_SECRET_PATH"
    
    # Retrieve the secret from Vault
    local vault_response
    vault_response=$(vault kv get -format=json "$VAULT_SECRET_PATH" 2>/dev/null) || {
        print_error "Failed to retrieve secret from path: $VAULT_SECRET_PATH"
        print_error "Please check if the path exists and you have read permissions"
        exit 1
    }
    
    # Extract credentials using jq
    MONGO_USERNAME=$(echo "$vault_response" | jq -r '.data.data.username // .data.username // empty')
    MONGO_PASSWORD=$(echo "$vault_response" | jq -r '.data.data.password // .data.password // empty')
    
    # Check if credentials were found
    if [ -z "$MONGO_USERNAME" ] || [ -z "$MONGO_PASSWORD" ]; then
        print_error "MongoDB credentials not found in Vault secret"
        print_error "Expected fields: 'username' and 'password'"
        echo "Available fields:"
        echo "$vault_response" | jq -r '.data.data // .data | keys[]' 2>/dev/null || echo "Unable to parse secret data"
        exit 1
    fi
    
    print_status "Successfully retrieved MongoDB credentials"
}

# Function to construct MongoDB connection URI
build_mongo_uri() {
    # URL encode username and password
    local encoded_username=$(printf '%s' "$MONGO_USERNAME" | jq -sRr @uri)
    local encoded_password=$(printf '%s' "$MONGO_PASSWORD" | jq -sRr @uri)
    
    # Build the connection URI
    MONGO_URI="mongodb://${encoded_username}:${encoded_password}@${MONGO_HOST}:${MONGO_PORT}/${MONGO_DATABASE}?authSource=${MONGO_AUTH_SOURCE}"
}

# Function to connect to MongoDB
connect_mongodb() {
    print_status "Connecting to MongoDB at $MONGO_HOST:$MONGO_PORT"
    print_status "Database: $MONGO_DATABASE"
    print_status "Auth Source: $MONGO_AUTH_SOURCE"
    print_status "Username: $MONGO_USERNAME"
    
    # Test connection first
    print_status "Testing connection..."
    if mongosh "$MONGO_URI" --eval "db.runCommand({ping: 1})" --quiet > /dev/null 2>&1; then
        print_status "Connection test successful!"
    else
        print_error "Connection test failed. Please check your credentials and MongoDB server status."
        exit 1
    fi
    
    # Connect interactively
    print_status "Starting MongoDB shell..."
    echo "======================================"
    mongosh "$MONGO_URI"
}

# Function to display usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Environment Variables:"
    echo "  VAULT_ADDR          Vault server address (default: http://127.0.0.1:8200)"
    echo "  VAULT_TOKEN         Vault authentication token"
    echo "  VAULT_SECRET_PATH   Path to MongoDB secret in Vault (default: secret/mongodb)"
    echo "  MONGO_HOST          MongoDB host (default: localhost)"
    echo "  MONGO_PORT          MongoDB port (default: 27017)"
    echo "  MONGO_DATABASE      Database to connect to (default: admin)"
    echo "  MONGO_AUTH_SOURCE   Authentication database (default: admin)"
    echo ""
    echo "Examples:"
    echo "  # Basic usage with defaults"
    echo "  $0"
    echo ""
    echo "  # Custom Vault path and MongoDB host"
    echo "  VAULT_SECRET_PATH=secret/prod/mongodb MONGO_HOST=prod-mongo.example.com $0"
    echo ""
    echo "  # Using Vault token"
    echo "  VAULT_TOKEN=hvs.xyz123 $0"
}

# Main function
main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_status "Starting MongoDB Vault Connection Script"
    
    # Check dependencies
    check_dependencies
    
    # Authenticate with Vault
    vault_auth
    
    # Get credentials from Vault
    get_credentials
    
    # Build MongoDB URI
    build_mongo_uri
    
    # Connect to MongoDB
    connect_mongodb
}

# Cleanup function
cleanup() {
    # Clear sensitive variables
    unset MONGO_USERNAME
    unset MONGO_PASSWORD
    unset MONGO_URI
    unset VAULT_TOKEN
}

# Set trap for cleanup on exit
trap cleanup EXIT

# Run main function
main "$@"