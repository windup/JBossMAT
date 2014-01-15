function toggle(div_id){
	var div = document.getElementById(div_id);
	if(div){
		// alert(div.style.display);
		if(div.style.display == 'none'){
			div.style.display = '';
		}else{
			div.style.display = 'none';
		}
	}else{
		alert("no div for " + div_id);
	}
}

function searchMbeans(){
	document.getElementById('status').innerHTML = 'filtering...';
	var q = document.getElementById('q').value.toLowerCase();
	var els = document.getElementById('wrapper').getElementsByTagName('div');
	var msg = "";
	for(i=0; i<els.length; i++){
		if(els[i].id && els[i].id.indexOf('header_') == 0){
			msg += els[i].innerHTML
			if(els[i].innerHTML.toLowerCase().indexOf(q) >= 0){
				//show
				els[i].style.display = '';
			}else{
				//hide
				els[i].style.display = 'none';
			}
		}
	}
	document.getElementById('status').innerHTML = '';
}