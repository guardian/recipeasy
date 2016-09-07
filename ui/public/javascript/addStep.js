function newStepInput(n) {
  return '<div class="flex step step_' + n +'"><textarea id="steps_' + n + '" name="steps[' + n + ']" value="" class="form-control"></textarea><button type="button" class="btn btn-default btn-sm button-add"> <span class="glyphicon glyphicon-plus"></span></button><button type="button" class="btn btn-default btn-sm button-remove"><span class="glyphicon glyphicon-minus"></span></button></div>'
}

$('body').on("click", '.button-add', function(){

  var newStepLength = $(this).parent().parent().children().length

  //adds an empty box at the end with correct class name
  var stepInput = newStepInput(newStepLength)
  $(this).parent().parent().append(stepInput)

  //get values of subsequent steps
  var inputs = $(this).parent().nextAll().find('textarea');
  var inputValues = inputs.map(function(){
    return $(this).val()
  }).toArray()

  //add empty value for new step
  inputValues.unshift("")

  //shift all existing after new step down one
  $.each(inputs, function(i){
    //set the input and value
    $(this).val(inputValues[i]);
    $(this).attr('value', inputValues[i])
   })
})



$('body').on("click", '.button-remove', function(){

  //get value of subsequent steps
  var inputs = $(this).parent().nextAll().andSelf().find('textarea');
  var inputValues = inputs.map(function(){
    return $(this).val()
  }).toArray()

  //remove value being deleted
  inputValues.shift()

  //move all the values up one
  $.each(inputs, function(i){
    $(this).attr('value', inputValues[i]);
    $(this).val(inputValues[i])
  })

  $(this).parent().siblings().last().remove()


})
