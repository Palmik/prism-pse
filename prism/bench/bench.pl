use strict;
use warnings;

sub run_prism
{
  my ($prism, $args, $env_vars) = @_;
  
  my $env = "";
  for my $k (keys %{$env_vars})
  {
    $env .= "$k=$env_vars->{$k} ";
  }

  my $command = "env $env $prism $args 2>/dev/null";
  my $out = `$command`;
  
  if ($out =~ qr/Total time for model checking: ([0-9,.]*).*/)
  {
    return $1;
  }
  elsif ($out =~ qr/Time for parameter space exploration: ([0-9,.]*).*/)
  {
    return $1;
  } 
  else
  {
    die "Could not parse the prism output:\n$out";
  }
}

sub run_prism_bench
{
  my ($prism, $args, $rep) = @_;
 
  print "OCL=0 $prism $args\n";
  my $ocl0_avg = 0;
  for (1..$rep)
  {
    my $t = run_prism($prism, $args, { PRISM_JAVAMAXMEM => '3g', OCL => 0 });
    print "$t\n";
    $ocl0_avg += $t;
  }
  $ocl0_avg /= $rep;
  print "$ocl0_avg\n"; 
  
  print "OCL=1 $prism $args\n";
  my $ocl1_avg = 0;
  for (1..$rep)
  {
    my $t = run_prism($prism, $args, { PRISM_JAVAMAXMEM => '3g', OCL => 1, OCL_LWS => 64 });
    print "$t\n";
    $ocl1_avg += $t;
  }
  $ocl1_avg /= $rep;
  print "$ocl1_avg\n"; 
}

sub main
{
  my $def = do $ARGV[0];
  if (my $err = $@)
  {
    die $@;
  }
  my $rep = $ARGV[1];

  my @args = ();
  for my $model (@{$def->{models}})
  {
    for my $parameters (@{$model->{parameters}})
    {
      for my $option (@{$parameters->{options}})
      {
        if ($option->{pse})
        {
          my $args = "'$model->{path}' -pse $option->{pse}{time} $parameters->{value}";
          run_prism_bench($def->{prism}, $args, $rep);
        }
        elsif ($option->{psecheck})
        {
          my $args = "-psecheck $parameters->{value} '$model->{path}' -csl '$option->{psecheck}{csl}'";
          run_prism_bench($def->{prism}, $args, $rep);
        }
        else
        {
          die 'Unknown option type.';
        }
      }
    }
  }
}

main();
