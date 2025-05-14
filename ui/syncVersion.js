const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const version = fs.readFileSync(path.resolve(__dirname, '../VERSION'), 'utf8').trim();
execSync(`npm version --no-git-tag-version --allow-same-version ${version} `);
