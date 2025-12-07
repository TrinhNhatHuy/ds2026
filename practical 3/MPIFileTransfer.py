from mpi4py import MPI
import sys
import os

CHUNK_SIZE = 4096
TAG_FILENAME = 1
TAG_FILESIZE = 2
TAG_DATA = 3
TAG_END = 4


def sender(comm, filename):
    try:
        if not os.path.exists(filename):
            print(f"Error: File '{filename}' not found!")
            comm.send(None, dest=1, tag=TAG_FILENAME)
            return False
        
        filesize = os.path.getsize(filename)
        print(f"Sender (Rank 0): Sending file '{filename}' ({filesize} bytes)")
        
        comm.send(filename, dest=1, tag=TAG_FILENAME)
        
        comm.send(filesize, dest=1, tag=TAG_FILESIZE)
        
        total_sent = 0
        with open(filename, 'rb') as f:
            while True:
                chunk = f.read(CHUNK_SIZE)
                if not chunk:
                    break
                
                comm.send(chunk, dest=1, tag=TAG_DATA)
                total_sent += len(chunk)
                
                progress = (total_sent * 100.0) / filesize
                print(f"\rProgress: {progress:.2f}% ({total_sent}/{filesize} bytes)", 
                      end='', flush=True)
       
        comm.send(None, dest=1, tag=TAG_END)
        
        print(f"\nSender (Rank 0): File sent successfully!")
        return True
        
    except Exception as e:
        print(f"Sender error: {e}")
        return False


def receiver(comm):
    try:
        filename = comm.recv(source=0, tag=TAG_FILENAME)
        
        if filename is None:
            print("Receiver (Rank 1): No file to receive (sender error)")
            return False
        
        filesize = comm.recv(source=0, tag=TAG_FILESIZE)
        
        print(f"Receiver (Rank 1): Receiving file '{filename}' ({filesize} bytes)")
        
        output_filename = f"received_{filename}"
       
        total_received = 0
        with open(output_filename, 'wb') as f:
            while True:
                # Receive with status to check tag
                status = MPI.Status()
                data = comm.recv(source=0, tag=MPI.ANY_TAG, status=status)
                
                # Check if end signal
                if status.Get_tag() == TAG_END:
                    break
                
                # Write chunk to file
                if data:
                    f.write(data)
                    total_received += len(data)
                    
                    progress = (total_received * 100.0) / filesize
                    print(f"\rProgress: {progress:.2f}% ({total_received}/{filesize} bytes)", 
                          end='', flush=True)
        
        print(f"\nReceiver (Rank 1): File received successfully as '{output_filename}'")
        print(f"Receiver (Rank 1): Total received: {total_received} bytes")
        
        if total_received == filesize:
            print("Receiver (Rank 1): File size verified!")
        else:
            print(f"Receiver (Rank 1): Warning - Size mismatch! Expected {filesize}, got {total_received}")
        
        return True
        
    except Exception as e:
        print(f"Receiver error: {e}")
        return False


def main():
    # Initialize MPI
    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()
    size = comm.Get_size()
    
    if size != 2:
        if rank == 0:
            print(f"Error: This program requires exactly 2 processes")
            print(f"Usage: mpirun -np 2 python {sys.argv[0]} <filename>")
        comm.Abort(1)
        return
    
    # Rank 0
    if rank == 0:
        if len(sys.argv) > 1:
            filename = sys.argv[1]
        else:
            filename = input("Enter filename to send: ")
        
        # Send the file
        sender(comm, filename)
    
    # Rank 1
    elif rank == 1:
        receiver(comm)
    
    # Synchronize all processes
    comm.Barrier()
    
    if rank == 0:
        print("Transfer complete!")


if __name__ == "__main__":
    main()
