require 'socket'
require 'time'

port = ARGV.size > 0 ? ARGV[0] : 5432
server = TCPServer.new port
clients = []

protocol_re = /^(?<header>ROUTE:(([a-zA-Z0-9\-\.:,]+)|([*]));TYPE:(TEXT|FILE|FILE_PIECE|GRAPH);(FILE:(.+);)?FROM_ADDR:([a-zA-Z0-9\.:\-]+);FROM_NAME:([a-zA-Z\d\.\:]+);)(?<content>.*)/m

puts "Started server on port #{port}"

loop do
	Thread.new(server.accept) do |client|
		clients << client
		puts "#{Time.now} New client connected! Yay!"

		prev_header = nil

		loop do
			begin
				data, addr = client.recvfrom(1024)

				next if data.strip.size < 1 

				unless data =~ /^ROUTE/
					next if prev_header.nil?

					data = prev_header + data

					prev_header = nil
				end

				messages = data.split('ROUTE:').reject {|e| e.size < 1}.map {|e| protocol_re.match('ROUTE:' + e)}.compact

				next if messages.size < 1

				prev_header = messages[messages.size - 1]['header']

				messages.each do |ms|
					puts ">> #{ms['header']}\n\n#{ms['content']}\n\n"
				end

				clients.each do |c| 
					next if c == client

					begin
						messages.each {|m| c.write m[0]}
					rescue Exception => e
						clients.delete c
					end
				end
			rescue => e
				puts "Caught exception #{e.message} #{e.backtrace}"
			end
		end
	end
end
