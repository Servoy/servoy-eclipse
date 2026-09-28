cd projects
FOR /D %%G in (*) DO (
cd %%G
npm install
cd..)
cd ..
pause
