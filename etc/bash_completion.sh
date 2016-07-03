_hale()
{
  compopt +o default

  local cur="${COMP_WORDS[COMP_CWORD]}"
  
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

  #TODO support also simple case of value list returned by hale?
  # COMPREPLY=( $(compgen -W "${values}" -- ${cur}) )
}

complete -o default -F _hale hale

