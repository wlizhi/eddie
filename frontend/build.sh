rm -rf dist
npm run build
rm -rf ../ai-app/target
rm -rf ../ai-app/src/main/resources/static/*
cp -r dist/* ../ai-app/src/main/resources/static/