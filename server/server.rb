require 'socket'
require 'time'

port = ARGV.size > 0 ? ARGV[0] : 5432
server = TCPServer.new port
clients = []

puts "Started server on port #{port}"

loop do
	Thread.new(server.accept) do |client|
		clients << client
		puts "#{Time.now} New client connected! Yay!"

		loop do
			begin
				data, addr = client.recvfrom(1024)

				next if data.strip.size < 1 

				unless data =~ /^ROUTE/
					next
				end

				puts ">> #{data}"

				clients.each do |c| 
					next if c == client

					begin
						c.write data
					rescue Exception => e
						clients.delete c
					end
				end
			rescue => e
				puts "Caught exception #{e.message}"
			end
		end
	end
end
