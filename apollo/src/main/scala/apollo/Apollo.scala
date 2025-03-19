package apollo 

import scala.sys.process._

object Apollo {        
    def noNeedForThisExisting(): String = {
        val cmd = "uname -a" 
        val output = cmd.!! 
        println(output)
        output
    }

}