document.getElementById("createBackupModal").addEventListener("click", listBackupVaults);
document.getElementById("restoreBackupModal").addEventListener("click", listRecoveryPoints);
document.getElementById("retentionPeriod").addEventListener("change", toggleRetentionDuration);
document.getElementById("createBackup").addEventListener("click", listRecoveryPoints);
var csrfValue = document.querySelector("meta[name='_csrf']").attributes['content'].value;
console.log(csrfValue)
document.getElementById("createBackupSubmit").addEventListener("click", function(e){
    console.log(csrfValue)
    var csrfEl = document.getElementById("createBackupSubmit");
    csrfEl.value = csrfValue;
    $('#resource-info-modal').modal( 'hide' )
});

function listRecoveryPoints() {
console.log("list recovery points")
fetch('/plugin/awsBackup/listRecoveryPoints')
    .then(response => {
    return response.json()
    })
    .then(users =>{
    console.log(users);
    var recoveryPoints = users.recoveryPoints
    var dynamicSelect = document.getElementById("recoveryPoint");
    removeOptions(dynamicSelect);
    var arrayLength = recoveryPoints.length;
    for (var i = 0; i < arrayLength; i++) {
        console.log(recoveryPoints[i]);
        var newOption = document.createElement("option");
        newOption.text = recoveryPoints[i];
        dynamicSelect.add(newOption);
        }
    });
}

function triggerBackup() {
console.log("triggered backup")
fetch('/plugin/awsBackup/createBackup')
    .then(response => {
    return response.json()
    })
    .then(users =>{
    console.log(users);
    });
}

function listBackupVaults() {
fetch('/plugin/awsBackup/backupVaults')
    .then(response => {
    return response.json()
    })
    .then(users =>{
    console.log(users.vaults);
    var vaults = users.vaults
    var dynamicSelect = document.getElementById("backupVault");
    removeOptions(dynamicSelect);
    var arrayLength = vaults.length;
    for (var i = 0; i < arrayLength; i++) {
        console.log(vaults[i]);
        var newOption = document.createElement("option");
        newOption.text = vaults[i];
        dynamicSelect.add(newOption);
        }
    });
}

function removeOptions(selectElement) {
    var i, L = selectElement.options.length - 1;
    for(i = L; i >= 0; i--) {
        selectElement.remove(i);
    }
}

function toggleRetentionDuration() {
var x = document.getElementById("retentionDuration");
var period = document.getElementById("retentionPeriod")
console.log(period.value)
if (period.value !== "always") {
x.hidden = false;
} else {
x.hidden = true;   
}
}
