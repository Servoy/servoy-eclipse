cd projects
FOR /D %%G in (*) DO (
cd %%G
npm install --legacy-peer-deps
cd..)
cd ..
pause