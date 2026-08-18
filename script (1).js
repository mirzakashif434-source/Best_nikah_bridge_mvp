const matches=[['Ayesha',29,'Pakistan','Family-oriented and looking for a sincere marriage.'],['Sara',27,'Saudi Arabia','Values honesty, respect and family.'],['Hiba',30,'Germany','Interested in a serious marriage.']];
function show(id){document.querySelectorAll('section').forEach(x=>x.classList.remove('active'));document.getElementById(id).classList.add('active');scrollTo(0,0);if(id==='matches')render()}
function home(){show('home')}
function save(){let n=name.value.trim(),a=age.value.trim(),c=country.value.trim();if(!n||!a||!c){alert('Please enter name, age and country.');return}localStorage.setItem('bnb',JSON.stringify({n,a,c,about:about.value}));alert('Profile saved!');show('matches')}
function render(){list.innerHTML=matches.map(m=>`<div class="match"><h3>👤 ${m[0]}, ${m[1]}</h3><div class="muted">${m[2]}</div><p>${m[3]}</p><button class="interest" onclick="alert('Interest sent to ${m[0]} (MVP demo).')">♡ Express Interest</button></div>`).join('')}
