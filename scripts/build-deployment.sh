#!/bin/bash
# Quick deployment script for Simplicity

set -e

echo "========================================="
echo "Simplicity - Deployment Artifact Builder"
echo "========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17+ first."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17+ required. Found Java $JAVA_VERSION"
    exit 1
fi

echo "✓ Java $JAVA_VERSION detected"
echo ""

# Menu
echo "Select deployment artifact to build:"
echo "1) Uberjar (standalone JAR file)"
echo "2) Docker image"
echo "3) Both"
echo ""
read -p "Choice (1-3): " choice

case $choice in
    1|3)
        echo ""
        echo "Building uberjar..."
        clojure -T:build uberjar
        
        if [ -f "target/simplicity-standalone.jar" ]; then
            SIZE=$(du -h target/simplicity-standalone.jar | cut -f1)
            echo ""
            echo "✓ Uberjar built successfully: target/simplicity-standalone.jar ($SIZE)"
            echo ""
            echo "Test locally:"
            echo "  java -jar target/simplicity-standalone.jar"
            echo ""
            echo "Deploy to server:"
            echo "  scp target/simplicity-standalone.jar user@server:/opt/simplicity/"
            echo ""
        else
            echo "❌ Uberjar build failed"
            exit 1
        fi
        ;;&
    
    2|3)
        # Check if Docker is installed
        if ! command -v docker &> /dev/null; then
            echo "❌ Docker not found. Please install Docker first."
            exit 1
        fi
        
        echo ""
        echo "Building Docker image..."
        docker build -t simplicity:latest .
        
        if [ $? -eq 0 ]; then
            IMAGE_SIZE=$(docker images simplicity:latest --format "{{.Size}}")
            echo ""
            echo "✓ Docker image built successfully: simplicity:latest ($IMAGE_SIZE)"
            echo ""
            echo "Test locally:"
            echo "  docker run -p 3000:3000 simplicity:latest"
            echo ""
            echo "Or use docker-compose:"
            echo "  docker-compose up -d"
            echo ""
            echo "Save image for deployment:"
            echo "  docker save simplicity:latest | gzip > simplicity-latest.tar.gz"
            echo ""
        else
            echo "❌ Docker build failed"
            exit 1
        fi
        ;;
    
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "========================================="
echo "Next Steps:"
echo "========================================="
echo ""
echo "1. Test the artifact locally"
echo "2. Review deployment docs: docs/deployment-cloudflare.md"
echo "3. Deploy to your server"
echo "4. Configure Cloudflare DNS and SSL"
echo "5. Monitor with /health endpoint"
echo ""
echo "For detailed instructions, see:"
echo "  - docs/deployment-cloudflare.md"
echo "  - docs/security.md"
echo ""
