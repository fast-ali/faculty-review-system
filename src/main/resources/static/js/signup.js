// Client-side password validation for signup form
(function(){
  'use strict';
  var form = document.getElementById('signup-form');
  if(!form) return;

  var password = document.getElementById('password');
  var confirm = document.getElementById('confirmPassword');
  var submit = document.getElementById('signup-submit');
  var criteria = {
    length: document.getElementById('pw-length'),
    upper: document.getElementById('pw-upper'),
    lower: document.getElementById('pw-lower'),
    digit: document.getElementById('pw-digit'),
    special: document.getElementById('pw-special')
  };

  function testPassword(pw){
    return {
      length: pw.length >= 8,
      upper: /[A-Z]/.test(pw),
      lower: /[a-z]/.test(pw),
      digit: /[0-9]/.test(pw),
      special: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(pw)
    };
  }

  function updateCriteria(pw){
    var res = testPassword(pw);
    Object.keys(res).forEach(function(k){
      var el = criteria[k];
      if(!el) return;
      if(res[k]){
        el.setAttribute('data-valid','true');
        el.style.color = 'green';
      } else {
        el.setAttribute('data-valid','false');
        el.style.color = '#666';
      }
    });
    return res;
  }

  function updateState(){
    var pw = password.value || '';
    var conf = confirm.value || '';
    var res = updateCriteria(pw);
    var allOk = Object.keys(res).every(function(k){ return res[k]; });
    var match = allOk && pw === conf && pw.length>0;
    submit.disabled = !match;
  }

  password.addEventListener('input', updateState);
  confirm.addEventListener('input', updateState);

  // Prevent bad submissions on the client (server-side validation still required!)
  form.addEventListener('submit', function(e){
    var pw = password.value || '';
    var conf = confirm.value || '';
    var res = testPassword(pw);
    var allOk = Object.keys(res).every(function(k){ return res[k]; });
    if(!allOk){
      e.preventDefault();
      document.getElementById('signup-error').textContent = 'Password does not meet the required criteria.';
      return false;
    }
    if(pw !== conf){
      e.preventDefault();
      document.getElementById('signup-error').textContent = 'Password and confirmation do not match.';
      return false;
    }
    // otherwise allow submission; server must still validate and hash the password
    return true;
  });
})();

