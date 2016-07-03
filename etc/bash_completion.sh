_hale()
{
  COMPREPLY=()
  
  # delegate to hale command
  local hale_result=$(hale --complete $COMP_CWORD ${COMP_WORDS[@]})
  COMPREPLY=( $(eval $hale_result) )
  return 0
}

complete -F _hale hale
