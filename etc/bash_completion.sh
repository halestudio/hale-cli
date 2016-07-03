_hale()
{
  compopt +o default
  
  # delegate to hale command
  local hale_result=$(hale --complete $COMP_CWORD ${COMP_WORDS[@]})

  case "${hale_result}" in
    # Reserved word FILE
    FILE)
      compopt -o default; COMPREPLY=()
      return 0
      ;;
    # In all other cases - interpret result as command
    *)
      COMPREPLY=( $(eval $hale_result) )
      return 0
      ;;
  esac
}

complete -o default -F _hale hale

