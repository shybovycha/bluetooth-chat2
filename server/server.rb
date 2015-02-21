require 'socket'

port = ARGV.size > 0 ? ARGV[0] : 5432
server = TCPServer.new port
clients = []

loop do
	Thread.new(server.accept) do |client|
		clients << client
		loop do
			begin
				data = client.read

				clients.each do |c| 
					next if c == client
					c.write data
				end
			rescue => e
				puts "Caught exception #{e.message}"
			end
		end
	end
end
