{ prism => '../bin/prism'
, models =>
  [ { path => 'models/signalling_2_v6_noreg_small.sm'
    , opts =>
      [ { parameters =>
          [ { k1 => [0.1, 0.5], k2 => [0.01, 0.015] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.5 [ F[100,100] (popR>=2 & popR<=12) ]', acc => 0.1 }
          ]
        }
      ]
    }
  , { path => 'models/signalling_2_v6_noreg.sm'
    , opts =>
      [ { parameters =>
          [ { k1 => [0.1, 0.5], k2 => [0.01, 0.015] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.4 [ F[100,100] (popRp<=20 & popHp>=30) ]', acc => 0.3 }
          ]
        }
      ]
    }
  , { path => 'models/AM_39.sm'
    , opts =>
      [ { parameters =>
          [ { k_r_id3 => [1,2], k_r_id4 => [80,90], k_r_id5 => [0.01,0.04] }
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.95 [F[100,100] (s_id0=14) & (s_id1=0)]', acc => 0.1 }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>16}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>32}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>64}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>128}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>32}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>64}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>128}
          ]
        }
      ]
    }
  , { path => 'models/sir.sm'
    , opts =>
      [ { parameters =>
          [ { ki => [0.00005, 0.003], kr => [0.005, 0.2] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>16}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>16}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P>=0.1 [ (popI>0) U[165,215] (popI=0) ]', acc => 0.1 }
          ]
        }
      ]
    }
  , { path => 'models/g1s-hill.sm'
    , opts =>
      [ { parameters =>
          [ { degrA => [0.005, 0.5], degrB => [0.05, 0.15] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>16}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>32}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>64}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>128}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>32}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>64}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>128}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P>=0.4 [ F[1000,1000] (B<2) ]', acc => 0.01 }
          ]
        }
      ]
    }
  , { path => 'models/cluster.sm'
    , opts =>
      [ { parameters =>
          [ { ws_fail => [0.005, 0.05], line_fail => [0.001, 0.005] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'R{"time_not_min"}<=0.1[ C<=100 ]', acc => 0.1 }
          ]
        }
      ]
    }
  , { path => 'models/polling.sm'
    , opts =>
      [ { parameters =>
          [ { gamma => [50, 500], mu => [0.5,5] }
          ]
        , environment =>
          [ {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'R{"waiting"}<=2.5[C<=25]', acc => 0.05 }
          ]
        }
      ]
    }
  ]
}
